package org.pressf5.dashboard.controller;

import lombok.RequiredArgsConstructor;
import org.pressf5.dashboard.dto.AvailableRoleResponse;
import org.pressf5.dashboard.service.AdminRoleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AdminRoleService adminRoleService;

    @GetMapping("/available-roles")
    public List<AvailableRoleResponse> getAvailableRoles() {
        return adminRoleService.getAvailableRoles();
    }
}
