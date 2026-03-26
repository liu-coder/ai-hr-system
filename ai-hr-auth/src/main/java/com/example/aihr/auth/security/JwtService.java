package com.example.aihr.auth.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import com.example.aihr.common.security.JwtKeyUtils;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
        String errorMessage = JwtKeyUtils.validateKeyStrength(props.getSecret());
        if (errorMessage != null) {
            throw new IllegalArgumentException("JWT密钥验证失败: " + errorMessage);
        }
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String issueAccessToken(String tenantId, String userId, String username, List<String> roles, Map<String, Object> abac) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.getAccessTokenTtlSeconds());

        return Jwts.builder()
                .issuer(props.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .subject(userId)
                .claim("tenantId", tenantId)
                .claim("username", username)
                .claim("roles", roles)
                .claim("abac", abac)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}

