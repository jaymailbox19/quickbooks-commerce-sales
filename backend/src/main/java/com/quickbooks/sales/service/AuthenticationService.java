package com.quickbooks.sales.service;

import com.quickbooks.sales.dto.LoginRequest;
import com.quickbooks.sales.dto.WorkspaceResponse;
import com.quickbooks.sales.model.AppUser;
import com.quickbooks.sales.model.TenantMembership;
import com.quickbooks.sales.repository.AppUserRepository;
import com.quickbooks.sales.repository.TenantMembershipRepository;
import com.quickbooks.sales.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AuthenticationService {
    private final AppUserRepository appUserRepository;
    private final TenantMembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthenticationService(
            AppUserRepository appUserRepository,
            TenantMembershipRepository membershipRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.appUserRepository = appUserRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResult login(LoginRequest request) {
        if (request == null || request.username() == null
                || request.password() == null) {
            throw unauthorized();
        }

        AppUser user = appUserRepository.findByEmailIgnoreCase(
                        request.username().trim())
                .filter(AppUser::isEnabled)
                .filter(candidate -> passwordEncoder.matches(
                        request.password(), candidate.getPasswordHash()))
                .orElseThrow(this::unauthorized);

        List<WorkspaceResponse> workspaces = getWorkspaces(user.getId());
        if (workspaces.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "The account is not assigned to a business workspace"
            );
        }

        return new LoginResult(
                jwtService.generateWorkspaceSelectionToken(user.getId(), user.getEmail()),
                workspaces
        );
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> getWorkspaces(Long userId) {
        return membershipRepository.findAllByUser_IdAndUser_EnabledTrue(userId)
                .stream()
                .map(membership -> new WorkspaceResponse(
                        membership.getTenant().getId(),
                        membership.getTenant().getName(),
                        membership.getRole().name()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public String selectWorkspace(Long userId, Long tenantId) {
        TenantMembership membership = membershipRepository
                .findByUser_IdAndTenant_IdAndUser_EnabledTrue(userId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You are not a member of this business workspace"
                ));

        return jwtService.generateActiveWorkspaceToken(
                userId,
                membership.getUser().getEmail(),
                membership.getTenant().getId(),
                membership.getRole().name()
        );
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password"
        );
    }

    public record LoginResult(String token, List<WorkspaceResponse> workspaces) {
    }
}
