package com.quickbooks.sales.controller;

import com.quickbooks.sales.dto.LoginRequest;
import com.quickbooks.sales.dto.SelectWorkspaceRequest;
import com.quickbooks.sales.dto.WorkspaceResponse;
import com.quickbooks.sales.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public AuthenticationService.LoginResult login(
            @Valid @RequestBody LoginRequest request) {
        return authenticationService.login(request);
    }

    @GetMapping("/workspaces")
    public List<WorkspaceResponse> workspaces(
            @AuthenticationPrincipal Jwt jwt) {
        requireSelectionToken(jwt);
        return authenticationService.getWorkspaces(userId(jwt));
    }

    @PostMapping("/workspaces/select")
    public Map<String, String> selectWorkspace(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SelectWorkspaceRequest request) {
        requireSelectionToken(jwt);
        String token = authenticationService.selectWorkspace(
                userId(jwt), request.tenantId());
        return Map.of("token", token);
    }

    private void requireSelectionToken(Jwt jwt) {
        if (jwt == null || !"workspace_selection".equals(
                jwt.getClaimAsString("token_type"))) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "A workspace-selection token is required"
            );
        }
    }

    private Long userId(Jwt jwt) {
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid user identity in token"
            );
        }
    }
}
