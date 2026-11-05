package org.pressf5.dashboard.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

public final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> deferredCsrfToken) {
        // Используем XOR-обработку для защиты от BREACH.
        xor.handle(request, response, deferredCsrfToken);

        // Принудительно создаём CSRF-токен и записываем его в cookie.
        deferredCsrfToken.get();
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        String headerValue = request.getHeader(csrfToken.getHeaderName());

        // SPA передаёт исходное значение токена в HTTP-заголовке.
        if (StringUtils.hasText(headerValue)) {
            return plain.resolveCsrfTokenValue(request, csrfToken);
        }

        // Для остальных случаев сохраняем XOR-обработку.
        return xor.resolveCsrfTokenValue(request, csrfToken);
    }
}
