package com.example.aihr.gateway.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

class JwtRelayFilterTest {
    private static final String SECRET = "MyStrongKey123!@&MyStrongKey123!@&";

    private JwtRelayFilter filter;

    @BeforeEach
    void setUp() {
        JwtProps props = new JwtProps();
        props.setIssuer("ai-hr-auth");
        props.setSecret(SECRET);
        filter = new JwtRelayFilter(props, new ObjectMapper());
    }

    @Test
    void rejectsMissingBearerTokenOnProtectedPath() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/v1/attendance/anomalies").build());

        filter.filter(exchange, passthroughChain()).block();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
    }

    @Test
    void relaysTrustedHeadersForValidToken() {
        String token = Jwts.builder()
                .issuer("ai-hr-auth")
                .subject("user-1")
                .claim("tenantId", "tenant-1")
                .claim("roles", List.of("ADMIN", "HR"))
                .claim("abac", Map.of("deptIds", List.of("d-1")))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();

        MockServerHttpRequest request = MockServerHttpRequest.get("/v1/attendance/anomalies")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-Tenant-Id", "spoofed")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, ex -> {
            ServerHttpRequest mutated = ex.getRequest();
            assertEquals("tenant-1", mutated.getHeaders().getFirst("X-Tenant-Id"));
            assertEquals("user-1", mutated.getHeaders().getFirst("X-User-Id"));
            assertEquals("ADMIN,HR", mutated.getHeaders().getFirst("X-Roles"));
            assertTrue(mutated.getHeaders().containsKey("X-Abac"));
            return Mono.empty();
        }).block();
    }

    @Test
    void allowsPublicPathWithoutToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/v1/auth/token").build());

        filter.filter(exchange, passthroughChain()).block();

        assertTrue(exchange.getResponse().getStatusCode() == null);
    }

    private GatewayFilterChain passthroughChain() {
        return exchange -> Mono.empty();
    }
}
