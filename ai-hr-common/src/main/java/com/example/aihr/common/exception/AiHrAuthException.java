package com.example.aihr.common.exception;

import com.example.aihr.common.web.ApiError.ErrorCode;

/**
 * 鉴权异常基类。
 */
public class AiHrAuthException extends RuntimeException {
    private final ErrorCode errorCode;

    public AiHrAuthException(ErrorCode errorCode) {
        super(errorCode == null ? null : errorCode.defaultMessage());
        this.errorCode = errorCode;
    }

    public AiHrAuthException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

