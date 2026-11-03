package org.pressf5.dashboard.service;

import lombok.RequiredArgsConstructor;
import org.pressf5.dashboard.dto.AvailableRoleResponse;
import org.pressf5.dashboard.repository.AdminRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminRoleService {

    private final AdminRoleRepository adminRoleRepository;

    public List<AvailableRoleResponse> getAvailableRoles() {
        return adminRoleRepository.findAvailableRoles()
                .stream()
                .map(role -> new AvailableRoleResponse(role.getCode(), role.getName()))
                .toList();
    }
}
