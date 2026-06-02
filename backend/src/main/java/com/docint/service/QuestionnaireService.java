package com.docint.service;

import com.docint.dto.request.QuestionnaireSubmitRequest;
import com.docint.dto.response.QuestionnaireDTO;
import com.docint.entity.QuestionnaireResponse;
import com.docint.entity.User;
import com.docint.exception.BadRequestException;
import com.docint.repository.QuestionnaireQuestionRepository;
import com.docint.repository.QuestionnaireResponseRepository;
import com.docint.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionnaireService {

    private static final String SOURCE_MANUAL = "MANUAL";
    private static final String SOURCE_AUTO_DOCUMENT = "AUTO_DOCUMENT";
    private static final int MAX_DOC_CHARS = 24_000;
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)(?:\\+?\\d[\\d\\s().-]{8,}\\d)");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[\\w._%+-]+@[\\w.-]+\\.[A-Za-z]{2,}\\b");

    private final QuestionnaireResponseRepository responseRepository;
    private final QuestionnaireQuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final DocumentParserService parserService;
    private final ChatClient chatClient;
    private final AuditService auditService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public QuestionnaireDTO get(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        QuestionnaireResponse response = responseRepository.findByUserId(user.getId()).orElse(null);
        Map<String, String> answers = response != null ? response.getAnswers() : Map.of();
        Map<String, String> answerSources = response != null ? response.getAnswerSources() : Map.of();

        return new QuestionnaireDTO(
                questionRepository.findByActiveTrueOrderBySortOrderAscCreatedAtAsc()
                        .stream()
                        .map(com.docint.dto.response.QuestionnaireQuestionDTO::from)
                        .toList(),
                answers,
                answerSources,
                user.isOnboardingCompleted()
        );
    }

    @Transactional
    public void save(String email, QuestionnaireSubmitRequest request) {
        User user = userRepository.findByEmail(email).orElseThrow();
        List<com.docint.entity.QuestionnaireQuestion> activeQuestions =
                questionRepository.findByActiveTrueOrderBySortOrderAscCreatedAtAsc();

        Map<String, String> sanitizedAnswers = sanitizeAnswers(request != null ? request.answers() : Map.of(), activeQuestions);
        validateAllQuestionsAnswered(sanitizedAnswers, activeQuestions);
        Map<String, String> sanitizedSources = sanitizeSources(
                request != null ? request.answerSources() : Map.of(),
                sanitizedAnswers
        );

        QuestionnaireResponse response = responseRepository.findByUserId(user.getId())
                .orElseGet(() -> QuestionnaireResponse.builder().userId(user.getId()).build());
        response.setAnswers(sanitizedAnswers);
        response.setAnswerSources(sanitizedSources);
        responseRepository.save(response);

        user.setOnboardingCompleted(true);
        userRepository.save(user);
        auditService.log(email, "QUESTIONNAIRE_SUBMITTED", email, sanitizedAnswers.size() + " answers");
    }

    public Map<String, String> autofillFromDocuments(MultipartFile[] files) {
        StringBuilder text = new StringBuilder();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            try {
                var parsed = parserService.parse(new ByteArrayInputStream(file.getBytes()), file.getOriginalFilename());
                text.append("\n\n=== ")
                        .append(file.getOriginalFilename())
                        .append(" ===\n")
                        .append(parsed.text());
            } catch (Exception e) {
                log.warn("Auto-fill: could not parse {}: {}", file.getOriginalFilename(), e.getMessage());
            }
            if (text.length() > MAX_DOC_CHARS) {
                break;
            }
        }

        if (text.length() == 0) {
            return Map.of();
        }

        String docText = text.length() > MAX_DOC_CHARS ? text.substring(0, MAX_DOC_CHARS) : text.toString();

        StringBuilder questionList = new StringBuilder();
        List<com.docint.entity.QuestionnaireQuestion> activeQuestions =
                questionRepository.findByActiveTrueOrderBySortOrderAscCreatedAtAsc();
        for (var question : activeQuestions) {
            questionList.append("- ")
                    .append(question.getQuestionKey())
                    .append(": ")
                    .append(question.getQuestionText())
                    .append("\n");
        }

        String prompt = """
                You are extracting USER PROFILE details from the document text below.
                These documents may be resumes, CVs, onboarding forms, profile sheets, or internal introduction documents.

                For each question key:
                - extract the best direct answer you can find from the document
                - prefer explicit values copied from the text
                - if the answer is not clearly present, return an empty string for that key
                - do not invent, infer, or guess missing values

                Return ONLY a flat JSON object where:
                - each property name is one of the provided keys
                - each property value is a plain string
                - do not wrap values in nested objects
                - do not include markdown, commentary, or code fences

                Questions:
                %s

                Document content:
                %s
                """.formatted(questionList, docText);

        try {
            String json = chatClient.prompt()
                    .system("You extract structured data and must return strict JSON only.")
                    .user(prompt)
                    .call()
                    .content();
            Map<String, String> parsed = parseJsonAnswers(json);
            Map<String, String> heuristicAnswers = heuristicAnswers(docText);
            heuristicAnswers.forEach(parsed::putIfAbsent);
            log.info("Questionnaire auto-fill extracted {} answers from {} active questions", parsed.size(), activeQuestions.size());
            return parsed;
        } catch (Exception e) {
            log.error("Auto-fill LLM call failed: {}", e.getMessage(), e);
            Map<String, String> heuristicAnswers = heuristicAnswers(docText);
            log.info("Questionnaire auto-fill fallback heuristics extracted {} answers", heuristicAnswers.size());
            return heuristicAnswers;
        }
    }

    private Map<String, String> parseJsonAnswers(String content) {
        if (content == null) {
            return Map.of();
        }

        String json = content.trim();
        if (json.startsWith("```")) {
            json = json.replaceAll("^```(json)?", "").replaceAll("```$", "").trim();
        }

        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start < 0 || end < 0 || end <= start) {
            return Map.of();
        }
        json = json.substring(start, end + 1);

        try {
            Map<?, ?> raw = objectMapper.readValue(json, Map.class);
            Map<String, String> result = new LinkedHashMap<>();
            for (var question : questionRepository.findByActiveTrueOrderBySortOrderAscCreatedAtAsc()) {
                String normalized = normalizeAnswerValue(raw.get(question.getQuestionKey()));
                if (normalized != null && !normalized.isBlank()) {
                    result.put(question.getQuestionKey(), normalized);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Could not parse auto-fill JSON: {}", e.getMessage());
            return Map.of();
        }
    }

    private String normalizeAnswerValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> mapValue) {
            Object nested = mapValue.get("answer");
            if (nested == null) nested = mapValue.get("value");
            if (nested == null) nested = mapValue.get("text");
            return nested != null ? nested.toString().trim() : null;
        }
        return value.toString().trim();
    }

    private Map<String, String> heuristicAnswers(String docText) {
        Map<String, String> result = new LinkedHashMap<>();
        String normalizedText = docText == null ? "" : docText;
        String lower = normalizedText.toLowerCase(Locale.ROOT);

        String phone = firstMatch(PHONE_PATTERN, normalizedText);
        if (phone != null) {
            result.put("phone_number", phone.trim());
        }

        String workMode = detectWorkMode(lower);
        if (workMode != null) {
            result.put("work_mode", workMode);
        }

        String timezone = detectTimezone(normalizedText, lower);
        if (timezone != null) {
            result.put("time_zone", timezone);
        }

        String language = detectPreferredLanguage(lower);
        if (language != null) {
            result.put("preferred_language", language);
        }

        String location = detectOfficeLocation(normalizedText);
        if (location != null) {
            result.put("office_location", location);
        }

        String title = detectJobTitle(normalizedText, lower);
        if (title != null) {
            result.put("job_title", title);
        }

        String department = detectDepartment(lower);
        if (department != null) {
            result.put("department", department);
        }

        return result;
    }

    private String firstMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group() : null;
    }

    private String detectWorkMode(String lowerText) {
        if (lowerText.contains("hybrid")) return "Hybrid";
        if (lowerText.contains("remote")) return "Remote";
        if (lowerText.contains("onsite") || lowerText.contains("on-site") || lowerText.contains("on site")) return "Onsite";
        return null;
    }

    private String detectTimezone(String text, String lowerText) {
        List<String> zones = List.of("IST", "EST", "PST", "CST", "MST", "UTC", "GMT", "CET", "BST", "EET");
        for (String zone : zones) {
            Pattern zonePattern = Pattern.compile("\\b" + zone + "\\b");
            String match = firstMatch(zonePattern, text);
            if (match != null) return match;
        }
        if (lowerText.contains("time zone")) {
            for (String line : text.split("\\R")) {
                if (line.toLowerCase(Locale.ROOT).contains("time zone")) {
                    String value = afterColonOrDash(line);
                    if (value != null) return value;
                }
            }
        }
        return null;
    }

    private String detectPreferredLanguage(String lowerText) {
        List<String> languages = List.of(
                "english", "hindi", "spanish", "french", "german", "japanese",
                "mandarin", "chinese", "arabic", "portuguese", "italian"
        );
        for (String language : languages) {
            if (lowerText.contains(language)) {
                return Character.toUpperCase(language.charAt(0)) + language.substring(1);
            }
        }
        return null;
    }

    private String detectOfficeLocation(String text) {
        for (String line : text.split("\\R")) {
            String lowerLine = line.toLowerCase(Locale.ROOT).trim();
            if (lowerLine.startsWith("location") || lowerLine.startsWith("based in") || lowerLine.startsWith("office")) {
                String value = afterColonOrDash(line);
                if (value != null) return value;
            }
        }
        return null;
    }

    private String detectJobTitle(String text, String lowerText) {
        String[] keywords = {"engineer", "developer", "architect", "manager", "analyst", "consultant", "specialist", "lead", "intern"};
        List<String> lines = List.of(text.split("\\R"));
        for (String line : lines) {
            String normalized = line.trim();
            String lineLower = normalized.toLowerCase(Locale.ROOT);
            for (String keyword : keywords) {
                if (lineLower.contains(keyword) && normalized.length() <= 80 && !lineLower.contains("@")) {
                    return normalized;
                }
            }
        }
        if (lowerText.contains("job title")) {
            for (String line : lines) {
                if (line.toLowerCase(Locale.ROOT).contains("job title")) {
                    String value = afterColonOrDash(line);
                    if (value != null) return value;
                }
            }
        }
        return null;
    }

    private String detectDepartment(String lowerText) {
        Map<String, String> departments = Map.ofEntries(
                Map.entry("engineering", "Engineering"),
                Map.entry("product", "Product"),
                Map.entry("marketing", "Marketing"),
                Map.entry("sales", "Sales"),
                Map.entry("finance", "Finance"),
                Map.entry("human resources", "Human Resources"),
                Map.entry("hr", "Human Resources"),
                Map.entry("operations", "Operations"),
                Map.entry("support", "Support"),
                Map.entry("design", "Design")
        );
        for (var entry : departments.entrySet()) {
            if (lowerText.contains(entry.getKey())) return entry.getValue();
        }
        return null;
    }

    private String afterColonOrDash(String line) {
        if (line == null) return null;
        String trimmed = line.trim();
        int colon = trimmed.indexOf(':');
        if (colon >= 0 && colon + 1 < trimmed.length()) {
            String value = trimmed.substring(colon + 1).trim();
            return value.isBlank() ? null : value;
        }
        int dash = trimmed.indexOf('-');
        if (dash >= 0 && dash + 1 < trimmed.length()) {
            String value = trimmed.substring(dash + 1).trim();
            return value.isBlank() ? null : value;
        }
        return null;
    }

    private Map<String, String> sanitizeAnswers(Map<String, String> answers,
                                                List<com.docint.entity.QuestionnaireQuestion> activeQuestions) {
        Set<String> allowedKeys = new HashSet<>();
        activeQuestions.forEach(q -> allowedKeys.add(q.getQuestionKey()));

        Map<String, String> sanitized = new LinkedHashMap<>();
        if (answers == null) {
            return sanitized;
        }

        answers.forEach((key, value) -> {
            if (key == null || !allowedKeys.contains(key) || value == null) {
                return;
            }
            String trimmed = value.trim();
            if (!trimmed.isBlank()) {
                sanitized.put(key, trimmed);
            }
        });
        return sanitized;
    }

    private void validateAllQuestionsAnswered(Map<String, String> answers,
                                              List<com.docint.entity.QuestionnaireQuestion> activeQuestions) {
        boolean hasMissing = activeQuestions.stream()
                .anyMatch(q -> !answers.containsKey(q.getQuestionKey()));
        if (hasMissing) {
            throw new BadRequestException("Please answer all questionnaire fields before continuing.");
        }
    }

    private Map<String, String> sanitizeSources(Map<String, String> answerSources, Map<String, String> answers) {
        Map<String, String> sanitized = new LinkedHashMap<>();
        answers.forEach((key, value) -> {
            String source = answerSources != null ? answerSources.get(key) : null;
            sanitized.put(key, SOURCE_AUTO_DOCUMENT.equals(source) ? SOURCE_AUTO_DOCUMENT : SOURCE_MANUAL);
        });
        return sanitized;
    }
}
