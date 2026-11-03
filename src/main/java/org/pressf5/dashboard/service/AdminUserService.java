package org.pressf5.dashboard.service;

import lombok.RequiredArgsConstructor;
import org.pressf5.dashboard.api.ConflictException;
import org.pressf5.dashboard.dto.AdminRegistrationRequest;
import org.pressf5.dashboard.dto.AdminRegistrationResponse;
import org.pressf5.dashboard.entity.AdminRole;
import org.pressf5.dashboard.entity.AdminUser;
import org.pressf5.dashboard.repository.AdminRoleRepository;
import org.pressf5.dashboard.repository.AdminUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final AdminUserRepository adminUserRepository;
    private final AdminRoleRepository adminRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AdminRegistrationResponse register(AdminRegistrationRequest request) {

        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (adminUserRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("Пользователь с таким именем уже существует");
        }

        if (adminUserRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Пользователь с таким email уже существует");
        }

        String roleCode = request.roleCode().name();

        if (adminUserRepository.existsByRole_CodeAndEnabledTrue(roleCode)) {
            throw new ConflictException("Выбранная роль уже занята");
        }

        AdminRole role = adminRoleRepository.findByCode(roleCode)
                .orElseThrow(() -> new IllegalArgumentException("Указанная роль не найдена"));

        AdminUser adminUser = new AdminUser();

        adminUser.setUsername(username);
        adminUser.setEmail(email);
        adminUser.setPasswordHash(passwordEncoder.encode(request.password()));
        adminUser.setRole(role);
        adminUser.setEnabled(true);

        AdminUser savedUser = adminUserRepository.save(adminUser);

        return new AdminRegistrationResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                role.getCode(),
                role.getName()
        );
    }
}