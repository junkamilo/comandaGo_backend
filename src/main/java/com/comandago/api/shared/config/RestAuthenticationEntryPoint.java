package com.comandago.api.shared.config;

import com.comandago.api.shared.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    public static final String INVALID_BEARER_ATTR = "invalidBearerToken";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        String message = Boolean.TRUE.equals(request.getAttribute(INVALID_BEARER_ATTR))
                ? "Token inválido o expirado"
                : "No autenticado";
        writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, message);
    }

    static void writeJson(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> body = ApiResponse.error(message);
        response.getWriter().write(String.format(
                "{\"success\":%s,\"message\":\"%s\",\"data\":null,\"timestamp\":\"%s\"}",
                body.success(),
                escape(body.message()),
                body.timestamp()));
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
