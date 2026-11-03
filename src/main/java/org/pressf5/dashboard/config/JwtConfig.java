package org.pressf5.dashboard.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    public SecretKey jwtSecretKey(JwtProperties properties) {
        byte[] secretBytes;

        try {
            secretBytes = Base64.getDecoder().decode(properties.getSecret());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("JWT_SECRET должен содержать корректный Base64-секрет", ex);
        }

        if (secretBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET должен содержать не менее 32 байт");
        }

        return new SecretKeySpec(secretBytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }
}