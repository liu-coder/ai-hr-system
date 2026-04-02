package com.example.aihr.common.web;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 将业务接口的返回值包进 {@link ApiResult}；actuator、静态错误页等路径不包装。
 */
@RestControllerAdvice
public class ApiResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(
            @NonNull MethodParameter returnType,
            @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            @NonNull MethodParameter returnType,
            @NonNull MediaType selectedContentType,
            @NonNull Class<? extends HttpMessageConverter<?>> selectedConverter,
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response) {
        String path = request.getURI().getPath();
        if (path.startsWith("/actuator") || path.startsWith("/error")) {
            return body;
        }
        if (body instanceof ApiResult<?> || body instanceof ApiError) {
            return body;
        }
        if (body instanceof org.springframework.core.io.Resource) {
            return body;
        }
        return ApiResult.ok(body);
    }
}
