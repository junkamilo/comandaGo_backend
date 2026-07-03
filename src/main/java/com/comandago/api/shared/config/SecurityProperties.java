package com.comandago.api.shared.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class SecurityProperties {

    private String secret;
    private long expirationMs;
    private long refreshExpirationMs;
}
