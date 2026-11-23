package com.sushilambekar.expensetracker.user;

import com.sushilambekar.expensetracker.security.CurrentUserId;
import com.sushilambekar.expensetracker.user.AuthDtos.AuthResponse;
import com.sushilambekar.expensetracker.user.AuthDtos.LoginRequest;
import com.sushilambekar.expensetracker.user.AuthDtos.RegisterRequest;
import com.sushilambekar.expensetracker.user.AuthDtos.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(summary = "Create an account and get an access token")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return service.register(request);
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Log in and get an access token")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "The logged-in user")
    public UserResponse me(@CurrentUserId Long userId) {
        return service.me(userId);
    }
}
