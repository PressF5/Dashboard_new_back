package org.pressf5.dashboard.service;

import lombok.RequiredArgsConstructor;
import org.pressf5.dashboard.api.UnauthorizedException;
import org.pressf5.dashboard.config.JwtProperties;
import org.pressf5.dashboard.dto.AdminLoginRequest;
import org.pressf5.dashboard.dto.AdminLoginResponse;
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

    @Transactional(readOnly = true)
    public AdminLoginResponse login(AdminLoginRequest request) {

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username().trim(), request.password()));
        } catch (AuthenticationException ex) {
            throw new UnauthorizedException("Неверное имя пользователя или пароль");
        }

        AdminUser adminUser = adminUserRepository
                .findByUsernameIgnoreCase(request.username().trim())
                .orElseThrow(() -> new UnauthorizedException("Неверное имя пользователя или пароль"));

        String accessToken = jwtService.generateAccessToken(adminUser);

        return new AdminLoginResponse(
                accessToken,
                "Bearer",
                jwtProperties.getAccessTokenTtl().toSeconds(),
                adminUser.getId(),
                adminUser.getUsername(),
                adminUser.getEmail(),
                adminUser.getRole().getCode(),
                adminUser.getRole().getName()
        );
    }
}