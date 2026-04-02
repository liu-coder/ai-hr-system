package com.example.aihr.common.web;

/**
 * 统一成功响应体；错误仍由 {@link GlobalExceptionHandler} 返回 {@link ApiError}。
 */
public record ApiResult<T>(String code, String message, T data) {

    public static final String SUCCESS_CODE = "0";

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(SUCCESS_CODE, "OK", data);
    }

    /**
     * 将 RestTemplate 等反序列化得到的 Map 形 {@code {code,message,data}} 解包为内层 data；否则原样返回。
     */
    public static Object unwrapData(Object body) {
        if (!(body instanceof java.util.Map<?, ?> m)) {
            return body;
        }
        if (!SUCCESS_CODE.equals(String.valueOf(m.get("code")))) {
            return body;
        }
        if (!m.containsKey("data")) {
            return body;
        }
        return m.get("data");
    }
}
