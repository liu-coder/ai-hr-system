package com.example.aihr.common.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

public class AuthContextFilter extends OncePerRequestFilter {
    private final ServiceJwtProps props;
    private final ObjectMapper om;
    private final SecretKey key;

    public AuthContextFilter(ServiceJwtProps props, ObjectMapper om) {
        this.props = props;
        this.om = om;
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            RequestContext ctx = parseFromGatewayHeaders(request);
            if (ctx == null) {
                ctx = parseFromBearerJwt(request);
            }
            if (ctx != null) {
                RequestContextHolder.set(ctx);
            }
            filterChain.doFilter(request, response);
        } finally {
            RequestContextHolder.clear();
        }
    }

    private RequestContext parseFromGatewayHeaders(HttpServletRequest request) {
        String tenantId = request.getHeader("X-Tenant-Id");
        String userId = request.getHeader("X-User-Id");
        if (tenantId == null || userId == null) {
            return null;
        }

        String rolesCsv = request.getHeader("X-Roles");
        List<String> roles = (rolesCsv == null || rolesCsv.isBlank())
                ? List.of()
                : List.of(rolesCsv.split(","));

        String abacB64 = request.getHeader("X-Abac");
        Map<String, Object> abac = Collections.emptyMap();
        if (abacB64 != null && !abacB64.isBlank()) {
            try {
                byte[] json = Base64.getDecoder().decode(abacB64);
                abac = om.readValue(json, new TypeReference<>() {});
            } catch (Exception ignored) {
                abac = Collections.emptyMap();
            }
        }
        return new RequestContext(tenantId, userId, roles, abac);
    }

    @SuppressWarnings("unchecked")
    private RequestContext parseFromBearerJwt(HttpServletRequest request) {
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) {
            return null;
        }
        String token = auth.substring("Bearer ".length()).trim();
        Claims claims = Jwts.parser()
                .requireIssuer(props.getIssuer())
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        String tenantId = String.valueOf(claims.get("tenantId"));
        String userId = claims.getSubject();
        List<String> roles = (List<String>) claims.get("roles", List.class);
        Map<String, Object> abac = (Map<String, Object>) claims.get("abac", Map.class);
        return new RequestContext(tenantId, userId, roles == null ? List.of() : roles, abac == null ? Map.of() : abac);
    }
}
