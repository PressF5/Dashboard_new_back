
package org.pressf5.dashboard.service;

import lombok.RequiredArgsConstructor;
import org.pressf5.dashboard.config.JwtProperties;
import org.pressf5.dashboard.entity.AdminRefreshToken;
import org.pressf5.dashboard.entity.AdminUser;
import org.pressf5.dashboard.repository.AdminRefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final AdminRefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Создание refresh token для администратора.
     */
    @Transactional
    public CreatedRefreshToken create(AdminUser adminUser) {

        String rawToken = generateToken();
        Instant expiresAt = Instant.now().plus(jwtProperties.getRefreshTokenTtl());

        AdminRefreshToken refreshToken = new AdminRefreshToken();
        refreshToken.setAdminUser(adminUser);
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setTokenFamily(UUID.randomUUID());
        refreshToken.setExpiresAt(expiresAt);

        refreshTokenRepository.save(refreshToken);

        return new CreatedRefreshToken(rawToken, expiresAt);
    }

    /**
     * Генерация криптографически стойкого случайного токена.
     */
    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256 хеширование исходного токена.
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", ex);
        }
    }

    /**
     * Результат создания refresh token.
     * Исходный токен существует только в памяти приложения.
     */
    public record CreatedRefreshToken(String value, Instant expiresAt) {}
}
