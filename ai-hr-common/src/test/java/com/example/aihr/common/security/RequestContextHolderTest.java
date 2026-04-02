package com.example.aihr.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.aihr.common.exception.AiHrAuthException;
import com.example.aihr.common.web.ApiError.ErrorCode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RequestContextHolderTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.clear();
    }

    @Test
    void getRequiredReturnsStoredContext() {
        RequestContext context = new RequestContext(
                "tenant-1",
                "user-1",
                List.of("ADMIN"),
                Map.of("department", "hr"));
        RequestContextHolder.set(context);

        assertSame(context, RequestContextHolder.get());
        assertSame(context, RequestContextHolder.getOptional());
        assertSame(context, RequestContextHolder.getRequired());
    }

    @Test
    void getRequiredThrowsWhenContextMissing() {
        AiHrAuthException exception = assertThrows(AiHrAuthException.class, RequestContextHolder::getRequired);

        assertEquals(ErrorCode.AUTH_MISSING_TOKEN, exception.getErrorCode());
        assertEquals("Missing authenticated request context", exception.getMessage());
    }

    @Test
    void clearRemovesStoredContext() {
        RequestContextHolder.set(new RequestContext("tenant-2", "user-2", List.of(), Map.of()));

        RequestContextHolder.clear();

        assertNull(RequestContextHolder.get());
        assertNull(RequestContextHolder.getOptional());
    }
}
