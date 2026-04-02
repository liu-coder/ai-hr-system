package com.example.aihr.aicore.tools;

/**
 * 工具调用结果封装（P0）。
 * - 失败不抛异常，而是返回 failed(reason)，由调用方进行 RAG 降级或追问补参。
 */
public record ToolResult<T>(
        boolean success,
        T data,
        String reason,
        Integer statusCode
) {
    public static <T> ToolResult<T> success(T data) {
        return new ToolResult<>(true, data, null, null);
    }

    public static <T> ToolResult<T> failed(String reason) {
        return new ToolResult<>(false, null, reason, null);
    }

    public static <T> ToolResult<T> failed(String reason, Integer statusCode) {
        return new ToolResult<>(false, null, reason, statusCode);
    }
}




