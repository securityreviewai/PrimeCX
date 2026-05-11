package com.primecx.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.primecx.dto.AgentPerformanceCoachDto;
import com.primecx.model.User;
import com.primecx.service.AgentPerformanceCoachService;
import com.primecx.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/coach")
@RequiredArgsConstructor
public class AgentCoachController {

    private final AgentPerformanceCoachService agentPerformanceCoachService;
    private final UserService userService;

    @GetMapping("/performance")
    @PreAuthorize("hasAnyRole('SUPPORT_EXECUTIVE', 'SUPPORT_MANAGER', 'SUPPORT_ADMIN')")
    public ResponseEntity<AgentPerformanceCoachDto> getPerformanceCoach(@AuthenticationPrincipal OidcUser oidcUser) {
        User user = userService.getUserByOktaId(oidcUser.getSubject());
        AgentPerformanceCoachDto dto = agentPerformanceCoachService.buildCoachInsights(user.getId(), user.getRole());
        return ResponseEntity.ok(dto);
    }
}
