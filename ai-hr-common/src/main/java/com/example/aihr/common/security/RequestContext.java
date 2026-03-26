package com.example.aihr.common.security;

import java.util.List;
import java.util.Map;

public record RequestContext(
        String tenantId,
        String userId,
        List<String> roles,
        Map<String, Object> abac
) {}
