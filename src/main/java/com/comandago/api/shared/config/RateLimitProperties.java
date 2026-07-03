package com.comandago.api.shared.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.rate-limit.login")
public class RateLimitProperties {

    private int maxAttempts = 5;
    private int windowSeconds = 60;
}
