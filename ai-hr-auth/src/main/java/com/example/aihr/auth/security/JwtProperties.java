package com.example.aihr.auth.security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "aihr.security.jwt")
public class JwtProperties {
    @NotBlank
    private String issuer;
    @NotBlank
    @Size(min = 32)
    private String secret;
    @Min(1)
    private long accessTokenTtlSeconds = 1800;
    private int rotationDays = 30;

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public long getAccessTokenTtlSeconds() { return accessTokenTtlSeconds; }
    public void setAccessTokenTtlSeconds(long accessTokenTtlSeconds) { this.accessTokenTtlSeconds = accessTokenTtlSeconds; }
    public int getRotationDays() { return rotationDays; }
    public void setRotationDays(int rotationDays) { this.rotationDays = rotationDays; }
}
