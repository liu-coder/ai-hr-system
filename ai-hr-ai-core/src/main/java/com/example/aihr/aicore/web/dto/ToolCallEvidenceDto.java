package com.example.aihr.aicore.web.dto;

/**
 * 工具调用证据：统一输出给前端，支持可解释回答。
 */
public record ToolCallEvidenceDto(
        String toolName,
        String status,
        /** 可选：工具调用日志 ID，便于追溯 */
        String logId,
        /** 工具返回结果（即 toolResult 本体） */
        Object result
) {}
