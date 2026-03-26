package com.example.aihr.common.security;

import com.example.aihr.common.exception.AiHrAuthException;
import com.example.aihr.common.web.ApiError.ErrorCode;

public final class RequestContextHolder {
    private static final ThreadLocal<RequestContext> HOLDER = new ThreadLocal<>();

    private RequestContextHolder() {}

    public static void set(RequestContext context) {
        HOLDER.set(context);
    }

    public static RequestContext get() {
        return HOLDER.get();
    }

    public static RequestContext getRequired() {
        RequestContext context = HOLDER.get();
        if (context == null) {
            throw new AiHrAuthException(ErrorCode.AUTH_MISSING_TOKEN, "Missing authenticated request context");
        }
        return context;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
