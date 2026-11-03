package org.pressf5.dashboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminLoginRequest(

        @NotBlank(message = "Имя пользователя обязательно")
        @Size(max = 100, message = "Имя пользователя не должно превышать 100 символов")
        String username,

        @NotBlank(message = "Пароль обязателен")
        String password
) {
}
