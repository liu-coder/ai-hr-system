package com.example.aihr.gateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKey;
import com.example.aihr.common.security.JwtKeyUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Validates Bearer JWT at the gateway boundary and only forwards trusted identity headers.
 */
@Component
public class JwtRelayFilter implements GlobalFilter, Ordered {
    private static final List<String> RELAY_HEADERS = List.of("X-Tenant-Id", "X-User-Id", "X-Roles", "X-Abac");

    private final JwtProps props;
    private final ObjectMapper om;
    private final SecretKey key;

    public JwtRelayFilter(JwtProps props, ObjectMapper om) {
        this.props = props;
        this.om = om;
        String errorMessage = JwtKeyUtils.validateKeyStrength(props.getSecret());
        if (errorMessage != null) {
            throw new IllegalArgumentException("JWT密钥验证失败: " + errorMessage);
        }
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public int getOrder() {
        return -10;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) {
            return unauthorized(exchange.getResponse(), "Missing bearer token");
        }

        String token = auth.substring("Bearer ".length()).trim();
        try {
            Claims claims = Jwts.parser()
                    .requireIssuer(props.getIssuer())
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String tenantId = claims.get("tenantId", String.class);
            String userId = claims.getSubject();
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) claims.get("roles", List.class);
            Object abac = claims.get("abac");

            if (isBlank(tenantId) || isBlank(userId)) {
                return unauthorized(exchange.getResponse(), "Token missing required identity claims");
            }

            String abacB64 = Base64.getEncoder().encodeToString(toJsonBytes(abac));
            String rolesCsv = roles == null ? "" : String.join(",", roles);

            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .headers(headers -> {
                        headers.remove("X-Tenant-Id");
                        headers.remove("X-User-Id");
                        headers.remove("X-Roles");
                        headers.remove("X-Abac");
                        headers.add("X-Tenant-Id", tenantId);
                        headers.add("X-User-Id", userId);
                        headers.add("X-Roles", rolesCsv);
                        headers.add("X-Abac", abacB64);
                    })
                    .build();

            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (Exception e) {
            return unauthorized(exchange.getResponse(), "Invalid bearer token");
        }
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/v1/auth/")
                || path.startsWith("/actuator/health")
                || path.startsWith("/actuator/info");
    }

    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"code\":\"AUTH_MISSING_TOKEN\",\"message\":\"" + message + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }

    private byte[] toJsonBytes(Object o) {
        try {
            return om.writeValueAsBytes(o == null ? java.util.Map.of() : o);
        } catch (JsonProcessingException e) {
            return "{}".getBytes(StandardCharsets.UTF_8);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
