package com.sajilni.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "app.jwt")
@Validated
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class JwtConfig {

    @NotBlank(message = "JWT issuer is required")
    private String issuer;

    @NotBlank(message = "JWT secret is required")
    private String secret;

    @NotNull(message = "JWT expiration duration is required")
    private Duration expirationMs = Duration.ofMinutes(30);

    @NotNull(message = "JWT refresh expiration duration is required")
    private Duration refreshExpirationMs = Duration.ofDays(7);

    private String privateKeyPath = "classpath:certs/private_key.pem";
    private String publicKeyPath = "classpath:certs/public_key.pem";
    private boolean generateKeysIfMissing = true;
}