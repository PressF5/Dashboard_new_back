package org.pressf5.dashboard.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.pressf5.dashboard.dto.*;
import org.pressf5.dashboard.service.AdminAuthService;
import org.pressf5.dashboard.service.AdminRoleService;
import org.pressf5.dashboard.service.AdminUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AdminRoleService adminRoleService;
    private final AdminUserService adminUserService;
    private final AdminAuthService adminAuthService;

    /**
     * Авторизация администратора.
     */
    @PostMapping("/login")
    public AdminLoginResponse login(@Valid @RequestBody AdminLoginRequest request) {
        return adminAuthService.login(request);
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