package com.example.aihr.common.exception;

import com.example.aihr.common.web.ApiError.ErrorCode;

/**
 * 业务异常基类。
 * 域服务只需 throw new AiHrBusinessException(ErrorCode.XXX) 即可。
 */
public class AiHrBusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public AiHrBusinessException(ErrorCode errorCode) {
        super(errorCode == null ? null : errorCode.defaultMessage());
        this.errorCode = errorCode;
    }

    public AiHrBusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

