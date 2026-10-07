package com.quickbooks.sales.controller;

import com.quickbooks.sales.dto.LoginRequest;
import com.quickbooks.sales.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;


import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtService jwtService;

    @Value("${app.demo.username}")
    private String demoUsername;

    @Value("${app.demo.password}")
    private String demoPassword;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody LoginRequest request) {
        if (!demoUsername.equals(request.username())
                || !demoPassword.equals(request.password())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Invalid username or password"
            );
        }

        return Map.of("token", jwtService.generateToken(request.username()));
    }
}
