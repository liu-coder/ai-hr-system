package com.example.aihr.aicore.web;

import java.util.regex.Pattern;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Component;
import com.example.aihr.common.exception.AiHrBusinessException;
import com.example.aihr.common.web.ApiError.ErrorCode;

/**
 * 输入验证器（防止 XSS、SQL 注入等攻击）
 */
@Component
public class InputValidator {
    
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "<script|</script>|javascript:|on\\w+\\s*=|<iframe|</iframe>|<object|</object>|<embed", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(union\\s+select|select\\s+.*\\s+from|insert\\s+into|update\\s+.*\\s+set|" +
        "delete\\s+from|drop\\s+(table|database)|truncate\\s+table|" +
        "exec\\s*\\(|execute\\s)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
        "\\.\\./|\\.\\.\\\\|%2e%2e%2f|%2e%2e/",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * 验证用户输入是否合法
     */
    public boolean isValidUserInput(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        
        // 检查 XSS
        if (XSS_PATTERN.matcher(input).find()) {
            return false;
        }
        
        // 检查 SQL 注入
        if (SQL_INJECTION_PATTERN.matcher(input).find()) {
            return false;
        }
        
        // 检查路径遍历
        if (PATH_TRAVERSAL_PATTERN.matcher(input).find()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 清理输入（HTML 转义）
     */
    public String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return StringEscapeUtils.escapeHtml4(input);
    }
    
    /**
     * 验证并清理输入，如果非法则抛出异常
     */
    public String validateAndSanitize(String input, String fieldName) {
        if (!isValidUserInput(input)) {
            throw new AiHrBusinessException(ErrorCode.PARAM_INVALID_ARGUMENT,
                fieldName + " 包含非法字符");
        }
        return sanitize(input);
    }
    
    /**
     * 验证长度
     */
    public void validateLength(String input, int maxLength, String fieldName) {
        if (input != null && input.length() > maxLength) {
            throw new AiHrBusinessException(ErrorCode.PARAM_INVALID_ARGUMENT,
                fieldName + " 超过最大长度限制 (" + maxLength + "字符)");
        }
    }
    
    /**
     * 验证租户 ID 格式（白名单）
     */
    public boolean isValidTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return false;
        }
        return tenantId.matches("^[a-zA-Z0-9_-]+$");
    }
    
    /**
     * 验证员工 ID 格式
     */
    public boolean isValidEmployeeId(String employeeId) {
        if (employeeId == null || employeeId.isBlank()) {
            return false;
        }
        return employeeId.matches("^[a-zA-Z0-9_-]+$");
    }
}
