package com.sushilambekar.expensetracker.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param secret     HMAC key for signing tokens, at least 32 characters. When blank, a random key is used.
 * @param expiration how long an access token stays valid
 */
@ConfigurationProperties("app.jwt")
public record JwtProperties(String secret, Duration expiration) {
}
