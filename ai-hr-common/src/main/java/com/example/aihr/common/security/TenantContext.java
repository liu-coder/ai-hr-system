package com.example.aihr.common.security;

/**
 * 租户上下文，用于存储和获取当前请求的租户ID
 */
public class TenantContext {
    private static final ThreadLocal<String> tenantId = new ThreadLocal<>();
    
    /**
     * 设置租户ID
     * @param id 租户ID
     */
    public static void setTenantId(String id) {
        tenantId.set(id);
    }
    
    /**
     * 获取租户ID
     * @return 租户ID
     */
    public static String getTenantId() {
        return tenantId.get();
    }
    
    /**
     * 清除租户ID
     */
    public static void clear() {
        tenantId.remove();
    }
}