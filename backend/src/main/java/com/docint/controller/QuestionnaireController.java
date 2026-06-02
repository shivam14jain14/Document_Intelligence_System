package com.docint.controller;

import com.docint.dto.request.QuestionnaireSubmitRequest;
import com.docint.dto.response.QuestionnaireDTO;
import com.docint.service.QuestionnaireService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/questionnaire")
@RequiredArgsConstructor
public class QuestionnaireController {

    private final QuestionnaireService questionnaireService;

    /** Question set + the user's current answers. */
    @GetMapping
    public ResponseEntity<QuestionnaireDTO> get(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(questionnaireService.get(user.getUsername()));
    }

    /** Save answers and mark onboarding complete. */
    @PostMapping
    public ResponseEntity<Void> submit(@RequestBody QuestionnaireSubmitRequest request,
                                       @AuthenticationPrincipal UserDetails user) {
        questionnaireService.save(user.getUsername(), request);
        return ResponseEntity.noContent().build();
    }

    /** Upload document(s) → AI extracts suggested answers (not saved; for the user to review). */
    @PostMapping("/autofill")
    public ResponseEntity<Map<String, String>> autofill(@RequestParam("files") MultipartFile[] files) {
        return ResponseEntity.ok(questionnaireService.autofillFromDocuments(files));
    }
}
