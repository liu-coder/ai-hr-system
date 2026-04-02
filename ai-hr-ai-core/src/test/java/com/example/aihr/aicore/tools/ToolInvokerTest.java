package com.example.aihr.aicore.tools;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ToolInvokerTest {

    @Test
    void testSuccessfulInvocation() {
        ToolInvoker invoker = new ToolInvoker(3, 1000);
        AtomicInteger callCount = new AtomicInteger(0);
        
        ToolResult<String> result = invoker.invoke("testTool", () -> {
            callCount.incrementAndGet();
            return "success";
        });
        
        assertTrue(result.success());
        assertEquals("success", result.data());
        assertEquals(1, callCount.get());
    }

    @Test
    void testRetryOnFailure() {
        ToolInvoker invoker = new ToolInvoker(3, 1000);
        AtomicInteger callCount = new AtomicInteger(0);
        
        ToolResult<String> result = invoker.invoke("testTool", () -> {
            int count = callCount.incrementAndGet();
            if (count < 3) {
                throw new RuntimeException("Temporary failure");
            }
            return "success";
        });
        
        assertTrue(result.success());
        assertEquals("success", result.data());
        assertEquals(3, callCount.get());
    }

    @Test
    void testFailureAfterMaxAttempts() {
        ToolInvoker invoker = new ToolInvoker(2, 1000);
        AtomicInteger callCount = new AtomicInteger(0);
        
        ToolResult<String> result = invoker.invoke("testTool", () -> {
            callCount.incrementAndGet();
            throw new RuntimeException("Persistent failure");
        });
        
        assertFalse(result.success());
        assertNull(result.data());
        assertEquals(2, callCount.get());
    }

    @Test
    void testNoRetryOn4xxError() {
        ToolInvoker invoker = new ToolInvoker(3, 1000);
        AtomicInteger callCount = new AtomicInteger(0);
        
        ToolResult<String> result = invoker.invoke("testTool", () -> {
            callCount.incrementAndGet();
            throw new org.springframework.web.client.HttpClientErrorException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "Bad request");
        });
        
        assertFalse(result.success());
        assertEquals(1, callCount.get());
    }

    @Test
    void testRetryOn5xxError() {
        ToolInvoker invoker = new ToolInvoker(3, 1000);
        AtomicInteger callCount = new AtomicInteger(0);
        
        ToolResult<String> result = invoker.invoke("testTool", () -> {
            int count = callCount.incrementAndGet();
            if (count < 3) {
                throw new org.springframework.web.client.HttpServerErrorException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
            }
            return "success";
        });
        
        assertTrue(result.success());
        assertEquals("success", result.data());
        assertEquals(3, callCount.get());
    }
}



