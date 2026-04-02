package com.example.aihr.common.security;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 租户过滤器，用于从请求中提取租户ID并存储到TenantContext
 */
public class TenantFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        try {
            // 从RequestContextHolder中获取租户ID
            // 这样可以复用现有的认证逻辑，确保租户ID的一致性
            if (RequestContextHolder.getOptional() != null) {
                String tenantId = RequestContextHolder.getRequired().tenantId();
                TenantContext.setTenantId(tenantId);
            }
            filterChain.doFilter(request, response);
        } finally {
            // 清理租户上下文，避免内存泄漏
            TenantContext.clear();
        }
    }
}