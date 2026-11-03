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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final AdminRefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Создание новой семьи refresh-токенов при авторизации.
     */
    @Transactional
    public CreatedRefreshToken create(AdminUser adminUser) {
        return create(adminUser, UUID.randomUUID());
    }

    /**
     * Ротация refresh-токена.
     *
     * При успешной проверке старый токен отзывается,
     * а вместо него создаётся новый токен той же семьи.
     */
    @Transactional
    public RotationResult rotate(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            return RotationResult.invalid();
        }

        AdminRefreshToken currentToken = refreshTokenRepository
                .findByTokenHashForUpdate(hashToken(rawToken))
                .orElse(null);

        if (currentToken == null) {
            return RotationResult.invalid();
        }

        // Повторное использование уже отозванного токена.
        if (currentToken.getRevokedAt() != null) {
            revokeFamily(currentToken.getTokenFamily());
            return RotationResult.reused();
        }

        // Проверяем срок действия.
        Instant now = Instant.now();

        if (!currentToken.getExpiresAt().isAfter(now)) {
            currentToken.setRevokedAt(now);
            return RotationResult.invalid();
        }

        AdminUser adminUser = currentToken.getAdminUser();

        // Отключённому администратору нельзя обновлять токены.
        if (!adminUser.isEnabled()) {
            revokeFamily(currentToken.getTokenFamily());
            return RotationResult.invalid();
        }

        // Создаём новый токен в той же семье.
        CreatedRefreshToken replacement = create(adminUser, currentToken.getTokenFamily());

        // Отзываем старый токен и связываем его с новым.
        currentToken.setRevokedAt(now);

        AdminRefreshToken replacementEntity = refreshTokenRepository
                .findByTokenHash(hashToken(replacement.value()))
                .orElseThrow(() -> new IllegalStateException("Created refresh token was not found"));

        currentToken.setReplacedBy(replacementEntity);

        refreshTokenRepository.save(currentToken);

        return RotationResult.success(replacement);
    }

    /**
     * Отзыв одного refresh-токена.
     */
    @Transactional
    public boolean revoke(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }

        AdminRefreshToken refreshToken = refreshTokenRepository
                .findByTokenHashForUpdate(hashToken(rawToken))
                .orElse(null);

        if (refreshToken == null || refreshToken.getRevokedAt() != null) {
            return false;
        }

        refreshToken.setRevokedAt(Instant.now());

        return true;
    }

    /**
     * Отзыв всех действующих токенов семьи.
     */
    private void revokeFamily(UUID tokenFamily) {

        Instant now = Instant.now();

        List<AdminRefreshToken> activeTokens =
                refreshTokenRepository.findAllByTokenFamilyAndRevokedAtIsNull(tokenFamily);

        activeTokens.forEach(token -> token.setRevokedAt(now));

        refreshTokenRepository.saveAll(activeTokens);
    }

    /**
     * Создание токена с указанной семьёй.
     */
    private CreatedRefreshToken create(AdminUser adminUser, UUID tokenFamily) {

        String rawToken = generateToken();
        Instant expiresAt = Instant.now().plus(jwtProperties.getRefreshTokenTtl());

        AdminRefreshToken refreshToken = new AdminRefreshToken();
        refreshToken.setAdminUser(adminUser);
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setTokenFamily(tokenFamily);
        refreshToken.setExpiresAt(expiresAt);

        refreshTokenRepository.saveAndFlush(refreshToken);

        return new CreatedRefreshToken(rawToken, expiresAt);
    }

    private String generateToken() {

        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hashToken(String token) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", ex);
        }
    }

    public record CreatedRefreshToken(String value, Instant expiresAt) {}

    public enum RotationStatus {
        SUCCESS,
        INVALID,
        REUSED
    }

    public record RotationResult(RotationStatus status, CreatedRefreshToken refreshToken) {

        public static RotationResult success(CreatedRefreshToken refreshToken) {
            return new RotationResult(RotationStatus.SUCCESS, refreshToken);
        }

        public static RotationResult invalid() {
            return new RotationResult(RotationStatus.INVALID, null);
        }

        public static RotationResult reused() {
            return new RotationResult(RotationStatus.REUSED, null);
        }
    }
}
