package com.example.aihr.aicore.tools;

import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.stereotype.Component;

/**
 * 统一重试封装（P0）。
 *
 * 约定：
 * - 最多重试 N 次（可配置）
 * - 4xx 不重试
 * - 5xx 重试
 * - 超时错误重试
 * - 使用指数退避策略
 * - 返回失败结果 ToolResult.failed(reason)，调用方自动走 RAG 降级
 */
@Component
public class ToolInvoker {
    private static final Logger log = LoggerFactory.getLogger(ToolInvoker.class);

    private final ExecutorService executor;
    private final int maxAttempts;
    private final long timeoutMs;
    private final long initialBackoffMs;

    @Autowired
    public ToolInvoker(
            @Value("${aihr.toolinvoker.max-attempts:3}") int maxAttempts,
            @Value("${aihr.toolinvoker.timeout-ms:5000}") long timeoutMs,
            @Value("${aihr.toolinvoker.initial-backoff-ms:100}") long initialBackoffMs) {
        this.executor = createThreadPool();
        this.maxAttempts = maxAttempts;
        this.timeoutMs = timeoutMs;
        this.initialBackoffMs = initialBackoffMs;
    }

    public ToolInvoker(int maxAttempts, long timeoutMs) {
        this(maxAttempts, timeoutMs, 100);
    }

    private ExecutorService createThreadPool() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(10);
        taskExecutor.setMaxPoolSize(50);
        taskExecutor.setQueueCapacity(100);
        taskExecutor.setRejectedExecutionHandler((r, e) -> {
            log.warn("Task rejected by thread pool: {}", r);
            throw new RejectedExecutionException("ToolInvoker thread pool overloaded");
        });
        taskExecutor.initialize();
        return taskExecutor.getThreadPoolExecutor();
    }

    public <T> ToolResult<T> invoke(String toolName, Supplier<T> supplier) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                CompletableFuture<T> f = CompletableFuture.supplyAsync(supplier, executor);
                T resp = f.orTimeout(timeoutMs, TimeUnit.MILLISECONDS).join();
                @SuppressWarnings("unchecked")
                T unwrapped = (T) unwrapRestEnvelope(resp);
                return ToolResult.success(unwrapped);
            } catch (Exception ex) {
                Throwable cause = unwrap(ex);

                if (cause instanceof TimeoutException || cause instanceof SocketTimeoutException) {
                    String reason = toolName + " timeout after " + timeoutMs + "ms (attempt=" + attempt + ")";
                    log.warn(reason);
                    if (attempt < maxAttempts) {
                        backoff(attempt);
                        continue;
                    }
                    return ToolResult.failed(reason, null);
                }

                if (cause instanceof HttpStatusCodeException hse) {
                    HttpStatusCode status = hse.getStatusCode();
                    String body = hse.getResponseBodyAsString();
                    String reason = toolName + " http " + status.value() + " (attempt=" + attempt + "): " + abbreviate(body);
                    int code = status.value();

                    // 4xx 不重试
                    if (code >= 400 && code < 500) {
                        return ToolResult.failed(reason, code);
                    }
                    // 5xx 重试
                    if (attempt < maxAttempts && code >= 500) {
                        backoff(attempt);
                        continue;
                    }
                    return ToolResult.failed(reason, code);
                }

                String reason = toolName + " call failed (attempt=" + attempt + "): " + String.valueOf(cause);
                log.warn(reason, ex);
                if (attempt < maxAttempts) {
                    backoff(attempt);
                    continue;
                }
                return ToolResult.failed(reason, null);
            }
        }
        return ToolResult.failed(toolName + " call failed after retries");
    }
    
    /**
     * 指数退避策略
     * @param attempt 当前重试次数
     */
    private void backoff(int attempt) {
        if (initialBackoffMs <= 0) {
            return;
        }
        try {
            long backoffMs = initialBackoffMs * (1L << (attempt - 1));
            // 最大退避时间为 10 秒
            backoffMs = Math.min(backoffMs, 10000);
            log.debug("Backing off for {}ms before next attempt", backoffMs);
            Thread.sleep(backoffMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Backoff interrupted", e);
        }
    }

    private static Throwable unwrap(Exception ex) {
        if (ex instanceof java.util.concurrent.CompletionException ce && ce.getCause() != null) {
            return ce.getCause();
        }
        if (ex.getCause() != null) return ex.getCause();
        return ex;
    }

    private static String abbreviate(String s) {
        if (s == null) return "";
        String t = s.trim();
        return t.length() > 240 ? t.substring(0, 240) + "..." : t;
    }

    /**
     * 与 {@code ApiResult.unwrapData} 等价：将 RestTemplate 反序列化的 {@code {code,message,data}} 解包为 data，
     * 避免 ToolInvoker 线程里依赖 common 中类的加载顺序。
     */
    private static Object unwrapRestEnvelope(Object body) {
        if (!(body instanceof Map<?, ?> m)) {
            return body;
        }
        if (!"0".equals(String.valueOf(m.get("code")))) {
            return body;
        }
        if (!m.containsKey("data")) {
            return body;
        }
        return m.get("data");
    }
}




