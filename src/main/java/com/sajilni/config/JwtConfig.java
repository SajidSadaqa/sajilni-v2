package com.sajilni.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtConfig {
    private String secret;
    private java.time.Duration expirationMs;
    private String issuer;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public java.time.Duration getExpirationMs() { return expirationMs; }
    public void setExpirationMs(java.time.Duration expirationMs) { this.expirationMs = expirationMs; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
}
