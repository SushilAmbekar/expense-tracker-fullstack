package com.sushilambekar.expensetracker.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 255) String email,
            // BCrypt only uses the first 72 bytes of a password
            @NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record UserResponse(Long id, String name, String email) {

        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getName(), user.getEmail());
        }
    }

    public record AuthResponse(String token, Instant expiresAt, UserResponse user) {
    }
}
