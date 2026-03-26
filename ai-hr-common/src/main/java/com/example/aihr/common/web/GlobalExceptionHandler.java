package com.example.aihr.common.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.example.aihr.common.web.ApiError.ErrorCode;
import com.example.aihr.common.exception.AiHrAuthException;
import com.example.aihr.common.exception.AiHrBusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.ResourceAccessException;
import java.net.SocketTimeoutException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private String traceId(HttpServletRequest req) {
        String t = req.getHeader("X-Trace-Id");
        if (t == null || t.isBlank()) return "trc-" + UUID.randomUUID();
        return t;
    }

    private ResponseEntity<ApiError> build(HttpStatus status, ErrorCode code, HttpServletRequest req, String overrideMessage, Exception ex) {
        String tid = traceId(req);
        String msg = overrideMessage != null && !overrideMessage.isBlank() ? overrideMessage : code.defaultMessage();

        // 每次都在日志里落 traceId（P0 要求）。
        if (ex != null) {
            log.error("ai-hr error code={} category={} traceId={} path={} msg={}",
                    code.code(), code.category(), tid, req.getRequestURI(), msg, ex);
        } else {
            log.error("ai-hr error code={} category={} traceId={} path={} msg={}",
                    code.code(), code.category(), tid, req.getRequestURI(), msg);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Trace-Id", tid);
        return new ResponseEntity<>(
                new ApiError(code.code(), code.category().name(), msg, tid, Instant.now()),
                headers,
                status
        );
    }

    @ExceptionHandler(AiHrAuthException.class)
    public ResponseEntity<ApiError> handleAuth(AiHrAuthException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, ex.getErrorCode(), req, ex.getMessage(), ex);
    }

    @ExceptionHandler(AiHrBusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(AiHrBusinessException ex, HttpServletRequest req) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getErrorCode(), req, ex.getMessage(), ex);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.PARAM_INVALID_ARGUMENT, req, ex.getMessage(), ex);
    }
    
    /**
     * P0: 资源访问异常处理（网络超时、连接失败等）
     */
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ApiError> handleResourceAccess(ResourceAccessException ex, HttpServletRequest req) {
        String tid = traceId(req);
        Throwable rootCause = getRootCause(ex);
        
        if (rootCause instanceof SocketTimeoutException) {
            log.warn("资源访问超时 traceId={} path={} msg={}", tid, req.getRequestURI(), ex.getMessage());
            return build(HttpStatus.GATEWAY_TIMEOUT, ErrorCode.AI_TOOL_TIMEOUT, req, "服务连接超时", ex);
        }
        
        log.error("资源访问失败 traceId={} path={} msg={}", tid, req.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.AI_TOOL_FAILED, req, "外部服务不可用", ex);
    }
    
    /**
     * 获取异常的根因
     */
    private Throwable getRootCause(Throwable ex) {
        Throwable cause = ex.getCause();
        while (cause != null && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause != null ? cause : ex;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.SYS_INTERNAL_ERROR, req, null, ex);
    }
}

