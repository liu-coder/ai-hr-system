package com.example.aihr.aicore.web.dto;

/**
 * 统一 chat 响应：工具调用日志 + RAG 命中证据 + 降级说明 + 追问提示，便于前端可解释展示。
 */
public record ChatResponseDto(
        String sessionId,
        String message,
        RouteDecisionDto route,
        /** 大模型是否可用（已配置 Key 且调用成功） */
        boolean llmAvailable,
        /** 未配置或不可用时的降级说明，供前端展示 */
        String fallbackReason,
        /** 工具调用证据（ATTENDANCE/SALARY 时有值） */
        ToolCallEvidenceDto toolCall,
        /** RAG 检索命中（POLICY_RAG 或工具缺参回退时有值） */
        Object ragResult,
        /** 大模型回复（仅 llmAvailable 时非空） */
        String llmResult,
        /** 一句话证据摘要，便于前端/AI 直接展示可解释回答 */
        String evidenceSummary,
        /** 缺参时生成的追问提示，供前端展示并引导用户补全（扩展点：智能补参与追问） */
        String followUpPrompt
) {}



