
package org.pressf5.dashboard.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.pressf5.dashboard.api.UnauthorizedException;
import org.pressf5.dashboard.dto.AdminLoginRequest;
import org.pressf5.dashboard.dto.AdminLoginResponse;
import org.pressf5.dashboard.dto.AdminRegistrationRequest;
import org.pressf5.dashboard.dto.AdminRegistrationResponse;
import org.pressf5.dashboard.dto.AvailableRoleResponse;
import org.pressf5.dashboard.service.AdminAuthService;
import org.pressf5.dashboard.service.AdminRoleService;
import org.pressf5.dashboard.service.AdminUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";

    private final AdminRoleService adminRoleService;
    private final AdminUserService adminUserService;
    private final AdminAuthService adminAuthService;

    @Value("${app.cookie.secure:false}")
    private boolean secureCookie;

    @Value("${app.cookie.same-site:Lax}")
    private String sameSite;

    /**
     * Инициализация CSRF-токена для frontend.
     */
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf() {
        return ResponseEntity.noContent().build();
    }

    /**
     * Авторизация администратора.
     */
    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        AdminAuthService.LoginResult result = adminAuthService.login(request);
        Duration maxAge = Duration.between(Instant.now(), result.refreshToken().expiresAt());

        ResponseCookie refreshCookie = ResponseCookie
                .from(REFRESH_COOKIE_NAME, result.refreshToken().value())
                .httpOnly(true)
                .secure(secureCookie)
                .path("/api/auth")
                .sameSite(sameSite)
                .maxAge(maxAge)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(result.response());
    }

    /**
     * Выход администратора из системы.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        adminAuthService.logout(refreshToken);

        ResponseCookie clearCookie = ResponseCookie
                .from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secureCookie)
                .path("/api/auth")
                .sameSite(sameSite)
                .maxAge(Duration.ZERO)
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .build();
    }

    /**
     * Обновление access и refresh токенов.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AdminLoginResponse> refresh(@CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("Refresh-токен отсутствует");
        }

        AdminAuthService.LoginResult result =
                adminAuthService.refresh(refreshToken);

        Duration maxAge = Duration.between(
                Instant.now(),
                result.refreshToken().expiresAt()
        );

        ResponseCookie refreshCookie = ResponseCookie
                .from(REFRESH_COOKIE_NAME, result.refreshToken().value())
                .httpOnly(true)
                .secure(secureCookie)
                .path("/api/auth")
                .sameSite(sameSite)
                .maxAge(maxAge)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(result.response());
    }

    /**
     * Получение списка свободных ролей для регистрации.
     */
    @GetMapping("/available-roles")
    public List<AvailableRoleResponse> getAvailableRoles() {
        return adminRoleService.getAvailableRoles();
    }

    /**
     * Регистрация нового администратора.
     */
    @PostMapping("/register")
    public ResponseEntity<AdminRegistrationResponse> register(@Valid @RequestBody AdminRegistrationRequest request) {
        AdminRegistrationResponse response = adminUserService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
