package com.example.aihr.common.security.aspect;

import com.example.aihr.common.exception.AiHrAuthException;
import com.example.aihr.common.security.RequestContext;
import com.example.aihr.common.security.RequestContextHolder;
import com.example.aihr.common.security.annotation.RequireRoles;
import com.example.aihr.common.web.ApiError;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
public class RoleAuthorizationAspect {
    
    @Before("@annotation(com.example.aihr.common.security.annotation.RequireRoles)")
    public void checkRoles(JoinPoint joinPoint) {
        // 获取当前请求上下文
        RequestContext context = RequestContextHolder.get();
        if (context == null) {
            throw new AiHrAuthException(ApiError.ErrorCode.AUTH_MISSING_TOKEN);
        }
        
        // 获取用户角色
        List<String> userRoles = context.roles();
        if (userRoles == null) {
            userRoles = List.of();
        }
        
        // 获取方法上的RequireRoles注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireRoles requireRoles = method.getAnnotation(RequireRoles.class);
        
        // 如果方法上没有注解，检查类上的注解
        if (requireRoles == null) {
            requireRoles = method.getDeclaringClass().getAnnotation(RequireRoles.class);
        }
        
        if (requireRoles == null) {
            return;
        }
        
        // 获取需要的角色
        String[] requiredRoles = requireRoles.value();
        boolean any = requireRoles.any();
        
        Set<String> userRoleSet = userRoles.stream().collect(Collectors.toSet());
        
        if (any) {
            // 任意一个角色即可
            boolean hasRole = false;
            for (String role : requiredRoles) {
                if (userRoleSet.contains(role)) {
                    hasRole = true;
                    break;
                }
            }
            if (!hasRole) {
                throw new AiHrAuthException(ApiError.ErrorCode.AUTH_INVALID_TOKEN, "权限不足，需要至少一个角色: " + String.join(", ", requiredRoles));
            }
        } else {
            // 需要所有角色
            for (String role : requiredRoles) {
                if (!userRoleSet.contains(role)) {
                    throw new AiHrAuthException(ApiError.ErrorCode.AUTH_INVALID_TOKEN, "权限不足，需要角色: " + role);
                }
            }
        }
    }
}
