package com.docint.security;

import com.docint.entity.User;
import com.docint.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Restricts incomplete users to the onboarding questionnaire until they submit it.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OnboardingGuardFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_PREFIXES = Set.of(
            "/api/auth/",
            "/api/questionnaire",
            "/actuator/health",
            "/actuator/info",
            "/swagger-ui/",
            "/swagger-ui.html",
            "/v3/api-docs/"
    );

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isAllowedPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            filterChain.doFilter(request, response);
            return;
        }

        userRepository.findByEmail(auth.getName()).ifPresent(user -> {
            if (shouldBlock(user)) {
                writeForbidden(response);
            }
        });

        if (response.isCommitted()) {
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isAllowedPath(String uri) {
        return ALLOWED_PREFIXES.stream().anyMatch(uri::startsWith);
    }

    private boolean shouldBlock(User user) {
        return !user.isOnboardingCompleted();
    }

    private void writeForbidden(HttpServletResponse response) {
        if (response.isCommitted()) return;
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        try {
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "title", "Forbidden",
                    "status", 403,
                    "detail", "Please complete your onboarding questionnaire before accessing the rest of the platform.",
                    "code", "ONBOARDING_REQUIRED",
                    "timestamp", Instant.now().toString()
            ));
        } catch (IOException e) {
            log.warn("Could not write onboarding-required response: {}", e.getMessage());
        }
    }
}
