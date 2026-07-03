package com.comandago.api.shared.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class JwtSecretValidator {

    private final SecurityProperties securityProperties;

    @PostConstruct
    void validate() {
        String secret = securityProperties.getSecret();
        if (!StringUtils.hasText(secret) || secret.length() < 32) {
            throw new IllegalStateException(
                    "APP_JWT_SECRET debe estar definido y tener al menos 32 caracteres. "
                            + "Ver .env.example o application-local.properties.example");
        }
    }
}
