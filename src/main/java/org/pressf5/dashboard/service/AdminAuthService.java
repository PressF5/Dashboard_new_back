
package org.pressf5.dashboard.service;

import lombok.RequiredArgsConstructor;
import org.pressf5.dashboard.api.UnauthorizedException;
import org.pressf5.dashboard.config.JwtProperties;
import org.pressf5.dashboard.dto.AdminLoginRequest;
import org.pressf5.dashboard.dto.AdminLoginResponse;
import org.pressf5.dashboard.dto.UserIdentity;
import org.pressf5.dashboard.entity.AdminUser;
import org.pressf5.dashboard.repository.AdminUserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AuthenticationManager authenticationManager;
    private final AdminUserRepository adminUserRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public LoginResult login(AdminLoginRequest request) {

        String username = request.username().trim();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.password()));
        } catch (AuthenticationException ex) {
            throw new UnauthorizedException("Неверное имя пользователя или пароль");
        }

        AdminUser adminUser = adminUserRepository
                .findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UnauthorizedException("Неверное имя пользователя или пароль"));

        String accessToken = jwtService.generateAccessToken(adminUser);

        RefreshTokenService.CreatedRefreshToken refreshToken =
                refreshTokenService.create(adminUser);

        AdminLoginResponse response = new AdminLoginResponse(
                accessToken,
                "Bearer",
                jwtProperties.getAccessTokenTtl().toSeconds(),
                adminUser.getId(),
                adminUser.getUsername(),
                adminUser.getEmail(),
                adminUser.getRole().getCode(),
                adminUser.getRole().getName()
        );

        return new LoginResult(response, refreshToken);
    }

    /**
     * Обновление access и refresh токенов.
     */
    public LoginResult refresh(String rawRefreshToken) {
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(rawRefreshToken);

        if (rotation.status() != RefreshTokenService.RotationStatus.SUCCESS) {
            throw new UnauthorizedException("Refresh-токен недействителен или отозван");
        }

        UserIdentity user = rotation.user();
        String accessToken = jwtService.generateAccessToken(user);

        AdminLoginResponse response = new AdminLoginResponse(
                accessToken,
                "Bearer",
                jwtProperties.getAccessTokenTtl().toSeconds(),
                user.id(),
                user.username(),
                user.email(),
                user.roleCode(),
                user.roleName()
        );

        return new LoginResult(response, rotation.refreshToken());
    }

    public record LoginResult(
            AdminLoginResponse response,
            RefreshTokenService.CreatedRefreshToken refreshToken
    ) {}
}
