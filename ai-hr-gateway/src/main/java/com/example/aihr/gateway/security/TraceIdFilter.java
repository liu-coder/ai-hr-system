package com.example.aihr.gateway.security;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关 TraceId 过滤器（P0）。
 * - 调用方已有 `X-Trace-Id` 则透传
 * - 否则生成 UUID 注入
 * - 全链路日志只需打印这一处的 ID
 */
@Component
public class TraceIdFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);

    @Override
    public int getOrder() {
        // 最高优先级（比 JwtRelayFilter(-10) 更靠前）
        return -30;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = "trc-" + UUID.randomUUID();
        }

        String path = exchange.getRequest().getURI().getPath();
        log.info("traceId={} path={}", traceId, path);

        ServerHttpRequest mutated = exchange.getRequest()
                .mutate()
                .header("X-Trace-Id", traceId)
                .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }
}

