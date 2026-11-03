package org.pressf5.dashboard.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.pressf5.dashboard.enums.AdminRoleCode;

public record AdminRegistrationRequest(

        @NotBlank(message = "Имя пользователя обязательно")
        @Size(min = 3, max = 100, message = "Имя пользователя должно содержать от 3 до 100 символов")
        String username,

        @NotBlank(message = "Email обязателен")
        @Email(message = "Некорректный формат email")
        @Size(max = 255, message = "Email не должен превышать 255 символов")
        String email,

        @NotBlank(message = "Пароль обязателен")
        @Size(min = 8, max = 72, message = "Пароль должен содержать от 8 до 72 символов")
        String password,

        @NotNull(message = "Необходимо выбрать роль")
        AdminRoleCode roleCode
) {
}
