package com.example.aihr.common.web;

import java.time.Instant;

public record ApiError(
        String code,
        String category,
        String message,
        String traceId,
        Instant timestamp
) {

    public enum ErrorCategory {
        AUTH, PARAM, ATT, SAL, AI, SYS
    }

    /**
     * 统一错误码枚举（共 16 个）。
     *
     * 约定：
     * - 六大分类：AUTH / PARAM / ATT / SAL / AI / SYS
     * - GlobalExceptionHandler：
     *   - 鉴权异常 -> 401
     *   - 业务异常 -> 422
     *   - 未知 -> 500
     */
    public enum ErrorCode {
        // AUTH
        AUTH_INVALID_TOKEN(ErrorCategory.AUTH, "AUTH_INVALID_TOKEN", "Invalid or expired token."),
        AUTH_MISSING_TOKEN(ErrorCategory.AUTH, "AUTH_MISSING_TOKEN", "Missing token."),

        // PARAM
        PARAM_INVALID_ARGUMENT(ErrorCategory.PARAM, "PARAM_INVALID_ARGUMENT", "Invalid request argument."),
        PARAM_MISSING_TOOL_ARGS(ErrorCategory.PARAM, "PARAM_MISSING_TOOL_ARGS", "Missing required tool arguments."),

        // ATT
        ATTENDANCE_RULE_NOT_FOUND(ErrorCategory.ATT, "ATTENDANCE_RULE_NOT_FOUND", "Attendance rule set not found."),
        ATTENDANCE_RULE_JSON_INVALID(ErrorCategory.ATT, "ATTENDANCE_RULE_JSON_INVALID", "Attendance ruleJson is invalid."),
        ATTENDANCE_RECORD_NOT_FOUND(ErrorCategory.ATT, "ATTENDANCE_RECORD_NOT_FOUND", "Attendance record not found."),

        // SAL
        SALARY_POLICY_NOT_FOUND(ErrorCategory.SAL, "SALARY_POLICY_NOT_FOUND", "Salary policy not found."),
        SALARY_POLICY_JSON_INVALID(ErrorCategory.SAL, "SALARY_POLICY_JSON_INVALID", "Salary policyJson is invalid."),
        SALARY_CALC_FAILED(ErrorCategory.SAL, "SALARY_CALC_FAILED", "Salary calculation failed."),

        // AI
        AI_TOOL_TIMEOUT(ErrorCategory.AI, "AI_TOOL_TIMEOUT", "Tool call timeout."),
        AI_TOOL_FAILED(ErrorCategory.AI, "AI_TOOL_FAILED", "Tool call failed."),
        AI_LLM_UNAVAILABLE(ErrorCategory.AI, "AI_LLM_UNAVAILABLE", "LLM is unavailable."),
        AI_RAG_FAILED(ErrorCategory.AI, "AI_RAG_FAILED", "RAG failed."),

        // SYS
        SYS_RATE_LIMIT_EXCEEDED(ErrorCategory.SYS, "SYS_RATE_LIMIT_EXCEEDED", "Rate limit exceeded."),
        SYS_INTERNAL_ERROR(ErrorCategory.SYS, "SYS_INTERNAL_ERROR", "Internal server error.");

        private final ErrorCategory category;
        private final String code;
        private final String defaultMessage;

        ErrorCode(ErrorCategory category, String code, String defaultMessage) {
            this.category = category;
            this.code = code;
            this.defaultMessage = defaultMessage;
        }

        public ErrorCategory category() {
            return category;
        }

        public String code() {
            return code;
        }

        public String defaultMessage() {
            return defaultMessage;
        }
    }
}

