package org.pressf5.dashboard.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private String issuer;
    private Duration accessTokenTtl;
    private Duration refreshTokenTtl;
    private String secret;
}