package com.sushilambekar.expensetracker.user;

import com.sushilambekar.expensetracker.category.CategoryService;
import com.sushilambekar.expensetracker.common.ConflictException;
import com.sushilambekar.expensetracker.common.NotFoundException;
import com.sushilambekar.expensetracker.security.JwtService;
import com.sushilambekar.expensetracker.security.JwtService.IssuedToken;
import com.sushilambekar.expensetracker.user.AuthDtos.AuthResponse;
import com.sushilambekar.expensetracker.user.AuthDtos.LoginRequest;
import com.sushilambekar.expensetracker.user.AuthDtos.RegisterRequest;
import com.sushilambekar.expensetracker.user.AuthDtos.UserResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CategoryService categoryService;
    private final Clock clock;
    /** Compared against when the email is unknown, so a login takes as long whether or not the account exists. */
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService,
            CategoryService categoryService, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.categoryService = categoryService;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalise(request.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        User user = users.save(new User(request.name().trim(), email, passwordEncoder.encode(request.password()),
                LocalDateTime.now(clock)));
        categoryService.createDefaults(user.getId());
        return authResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Optional<User> user = users.findByEmail(normalise(request.email()));
        String hash = user.map(User::getPasswordHash).orElse(dummyHash);
        boolean matches = passwordEncoder.matches(request.password(), hash);
        if (user.isEmpty() || !matches) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return authResponse(user.get());
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return users.findById(userId).map(UserResponse::from).orElseThrow(() -> NotFoundException.of("User", userId));
    }

    private AuthResponse authResponse(User user) {
        IssuedToken token = jwtService.issue(user);
        return new AuthResponse(token.value(), token.expiresAt(), UserResponse.from(user));
    }

    private static String normalise(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
