package com.example.aihr.aicore.web;

import com.example.aihr.aicore.config.ThreadPoolConfig;
import com.example.aihr.aicore.llm.DashScopeChatService;
import com.example.aihr.aicore.llm.ToolResultSummarizer;
import com.example.aihr.aicore.rag.PolicyRagService;
import com.example.aihr.aicore.ratelimit.RateLimitConfig;
import com.example.aihr.aicore.repo.ToolCallLogRepository;
import com.example.aihr.aicore.service.MetricsService;

import com.example.aihr.aicore.slot.SlotExtractor;
import com.example.aihr.aicore.tools.AttendanceToolClient;
import com.example.aihr.aicore.tools.SalaryToolClient;
import com.example.aihr.aicore.tools.ToolResult;
import com.example.aihr.aicore.web.dto.ChatResponseDto;
import com.example.aihr.aicore.web.dto.RouteDecisionDto;
import com.example.aihr.aicore.web.dto.ToolCallEvidenceDto;
import com.example.aihr.common.exception.AiHrBusinessException;
import com.example.aihr.common.security.RequestContext;
import com.example.aihr.common.security.RequestContextHolder;
import com.example.aihr.common.validation.InputValidator;
import com.example.aihr.common.web.ApiError.ErrorCode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * ChatApplicationService 是核心业务逻辑处理类，负责处理聊天请求、工具调用和LLM交互等功能。
 * <p>
 * 主要职责包括：
 * 1. 处理聊天请求，验证输入参数
 * 2. 路由请求到相应的工具或服务
 * 3. 执行工具调用，处理工具执行结果
 * 4. 调用LLM生成响应
 * 5. 构建响应结果，包括工具调用证据和LLM结果
 * <p>
 * 该服务还实现了监控指标，用于追踪请求处理时间、工具调用次数、LLM调用次数等。
 */
@Service
public class ChatApplicationService {
    private static final Logger logger = LoggerFactory.getLogger(ChatApplicationService.class);
    private final ExecutorService executorService;
    private final AttendanceToolClient attendanceTool;
    private final SalaryToolClient salaryTool;
    private final PolicyRagService rag;
    private final ChatRouteService routeService;
    private final ToolCallLogRepository toolCallLogRepo;
    private final SlotExtractor slotExtractor;
    private final ToolResultSummarizer toolResultSummarizer;
    private final ObjectProvider<DashScopeChatService> dashScopeChatService;
    private final RateLimitConfig rateLimitConfig;
    private final InputValidator inputValidator;
    private final ChatToolArgumentService chatToolArgumentService;
    private final ChatPromptService chatPromptService;
    private final MetricsService metricsService;
    private final org.springframework.core.env.Environment env;
    private final Timer chatRequestTimer;
    
    // 构造函数中使用配置的线程池
    public ChatApplicationService(
            @Qualifier("chatExecutorService") ExecutorService executorService,
            AttendanceToolClient attendanceTool,
            SalaryToolClient salaryTool,
            PolicyRagService rag,
            ChatRouteService routeService,
            ToolCallLogRepository toolCallLogRepo,
            SlotExtractor slotExtractor,
            ToolResultSummarizer toolResultSummarizer,
            ObjectProvider<DashScopeChatService> dashScopeChatService,
            RateLimitConfig rateLimitConfig,
            InputValidator inputValidator,
            ChatToolArgumentService chatToolArgumentService,
            ChatPromptService chatPromptService,
            MeterRegistry meterRegistry,
            ThreadPoolConfig.ThreadPoolMonitor threadPoolMonitor,
            MetricsService metricsService,
            org.springframework.core.env.Environment env) {
        this.executorService = executorService;
        this.attendanceTool = attendanceTool;
        this.salaryTool = salaryTool;
        this.rag = rag;
        this.routeService = routeService;
        this.toolCallLogRepo = toolCallLogRepo;
        this.slotExtractor = slotExtractor;
        this.toolResultSummarizer = toolResultSummarizer;
        this.dashScopeChatService = dashScopeChatService;
        this.rateLimitConfig = rateLimitConfig;
        this.inputValidator = inputValidator;
        this.chatToolArgumentService = chatToolArgumentService;
        this.chatPromptService = chatPromptService;
        this.metricsService = metricsService;
        this.env = env;
        
        // 打印 dashscope api-key 配置
        String apiKey = env.getProperty("spring.ai.dashscope.api-key");
        logger.info("spring.ai.dashscope.api-key: {}", apiKey);
        logger.info("spring.ai.dashscope.api-key is blank: {}", apiKey == null || apiKey.isBlank());
        logger.info("spring.ai.dashscope.api-key equals 'sk-placeholder': {}", "sk-placeholder".equals(apiKey));
        
        // 打印 DashScopeChatService 是否可用
        DashScopeChatService svc = dashScopeChatService.getIfAvailable();
        logger.info("DashScopeChatService is available: {}", svc != null);
        
        // 监控线程池状态
        threadPoolMonitor.monitor(executorService, "chat");
        
        // 初始化监控指标
        this.chatRequestTimer = Timer.builder("aihr.chat.requests.duration")
                .description("Chat request processing time")
                .register(meterRegistry);
    }

    /**
     * 处理聊天请求，执行相应的工具调用和LLM交互，返回聊天响应。
     * <p>
     * 该方法是核心入口方法，处理整个聊天流程，包括：
     * 1. 验证速率限制
     * 2. 获取或创建会话ID
     * 3. 验证用户输入
     * 4. 路由请求到相应的工具或服务
     * 5. 执行工具调用
     * 6. 调用LLM生成响应
     * 7. 构建响应结果
     * <p>
     * @param requestSessionId 请求的会话ID，如果为null则创建新的会话ID
     * @param message 用户输入的消息
     * @param requestToolArgs 工具调用的参数
     * @param traceId 跟踪ID，用于分布式追踪
     * @return 聊天响应，包含会话ID、原始消息、路由决策、LLM结果等信息
     */
    public ChatResponseDto handleChat(String requestSessionId, String message, Map<String, Object> requestToolArgs, String traceId) {
        return chatRequestTimer.record(() -> {
            metricsService.incrementActiveRequests();
            try {
                metricsService.recordChatRequest();
                
                RequestContext ctx = RequestContextHolder.getRequired();
                validateRateLimit(ctx);
                
                String sessionId = getOrCreateSessionId(requestSessionId);
                String text = validateUserInput(message);
                
                RouteDecisionDto routeDecision = routeService.decide(text);
                String route = routeDecision.route();
                Map<String, Object> effectiveToolArgs = chatToolArgumentService.buildEffectiveToolArgs(requestToolArgs, text, route, sessionId);
                
                ToolExecutionResult toolExecutionResult = executeTool(ctx, sessionId, traceId, route, effectiveToolArgs, text);
                
                String llmResult = callLLM(ctx, text, route, toolExecutionResult, routeDecision);
                boolean llmAvailable = llmResult != null;
                
                String fallbackReason = buildFallbackReason(llmAvailable, toolExecutionResult.isToolArgsMissing(), toolExecutionResult.getToolResult());
                
                ToolCallEvidenceDto toolCall = buildToolCallEvidence(sessionId, toolExecutionResult.getToolName(), toolExecutionResult.getToolResult());
                
                String evidenceSummary = chatPromptService.buildEvidenceSummary(routeDecision, toolCall, toolExecutionResult.getRagResult(), llmAvailable, toolExecutionResult.isToolArgsMissing());
                
                return new ChatResponseDto(
                        sessionId,
                        text,
                        routeDecision,
                        llmAvailable,
                        fallbackReason,
                        toolCall,
                        toolExecutionResult.getRagResult(),
                        llmResult,
                        evidenceSummary,
                        toolExecutionResult.getFollowUpPrompt()
                );
            } finally {
                metricsService.decrementActiveRequests();
            }
        });
    }
    
    private void validateRateLimit(RequestContext ctx) {
        if (!rateLimitConfig.tryAcquire(ctx.tenantId(), ctx.userId(), true)) {
            throw new AiHrBusinessException(ErrorCode.SYS_RATE_LIMIT_EXCEEDED);
        }
    }
    
    private String getOrCreateSessionId(String requestSessionId) {
        return requestSessionId == null || requestSessionId.isBlank()
                ? "cs-" + UUID.randomUUID()
                : requestSessionId;
    }
    
    private String validateUserInput(String message) {
        String text = message.trim();
        if (!inputValidator.isValidUserInput(text)) {
            throw new AiHrBusinessException(ErrorCode.PARAM_INVALID_ARGUMENT, "Message contains invalid characters");
        }
        inputValidator.validateLength(text, 2000, "message");
        return text;
    }
    
    private static class ToolExecutionResult {
        private final ToolResult<Object> toolResult;
        private final String toolName;
        private final Object ragResult;
        private final boolean toolArgsMissing;
        private final String followUpPrompt;
        
        public ToolExecutionResult(ToolResult<Object> toolResult, String toolName, Object ragResult, boolean toolArgsMissing, String followUpPrompt) {
            this.toolResult = toolResult;
            this.toolName = toolName;
            this.ragResult = ragResult;
            this.toolArgsMissing = toolArgsMissing;
            this.followUpPrompt = followUpPrompt;
        }
        
        public ToolResult<Object> getToolResult() {
            return toolResult;
        }
        
        public String getToolName() {
            return toolName;
        }
        
        public Object getRagResult() {
            return ragResult;
        }
        
        public boolean isToolArgsMissing() {
            return toolArgsMissing;
        }
        
        public String getFollowUpPrompt() {
            return followUpPrompt;
        }
    }
    
    /**
     * 执行工具调用，根据路由类型选择相应的工具并执行。
     * <p>
     * 该方法根据路由类型执行不同的工具调用：
     * 1. ATTENDANCE/ATTENDANCE_PATTERN：执行考勤相关工具
     * 2. ATTENDANCE_MULTI_SUMMARY：执行批量考勤查询
     * 3. SALARY：执行薪资相关工具
     * 4. SALARY_DIFF：执行薪资比较工具
     * 5. POLICY_RECOMMEND/COMPLIANCE_RISK：执行政策推荐和合规风险评估
     * <p>
     * 如果工具参数不完整，会设置工具参数缺失标志并构建后续提示。
     * <p>
     * @param ctx 请求上下文，包含租户ID、用户ID等信息
     * @param sessionId 会话ID
     * @param traceId 跟踪ID，用于分布式追踪
     * @param route 路由类型
     * @param effectiveToolArgs 有效的工具参数
     * @param text 用户输入的文本
     * @return 工具执行结果，包含工具调用结果、工具名称、RAG结果等信息
     */
    private ToolExecutionResult executeTool(RequestContext ctx, String sessionId, String traceId, String route, Map<String, Object> effectiveToolArgs, String text) {
        ToolExecutionResult result = null;
        
        try {
            if ("ATTENDANCE".equals(route) || "ATTENDANCE_PATTERN".equals(route)) {
                result = executeAttendanceTool(ctx, sessionId, traceId, route, effectiveToolArgs, text);
            } else if ("ATTENDANCE_MULTI_SUMMARY".equals(route)) {
                result = executeAttendanceMultiSummaryTool(ctx, sessionId, traceId, route, effectiveToolArgs, text);
            } else if ("SALARY".equals(route)) {
                result = executeSalaryTool(ctx, sessionId, traceId, route, effectiveToolArgs, text);
            } else if ("SALARY_DIFF".equals(route)) {
                result = executeSalaryDiffTool(ctx, sessionId, traceId, route, effectiveToolArgs, text);
            } else if ("POLICY_RECOMMEND".equals(route) || "COMPLIANCE_RISK".equals(route)) {
                result = executePolicyTool(ctx, sessionId, traceId, route, text);
            } else if ("POLICY_RAG".equals(route)) {
                result = executePolicyRagTool(ctx, text);
            } else {
                result = new ToolExecutionResult(null, null, null, false, null);
            }
            
            // 处理工具执行失败或参数缺失的情况
            if (result != null) {
                result = handleToolExecutionFailure(ctx, text, result);
            }
        } catch (Exception e) {
            logger.error("Tool execution failed: {}", e.getMessage(), e);
            metricsService.recordError("tool_execution");
            result = new ToolExecutionResult(ToolResult.failed("Tool execution failed: " + e.getMessage()), null, null, false, null);
        }
        
        return result;
    }
    
    /**
     * 执行考勤相关工具。
     * 
     * @param ctx 请求上下文
     * @param sessionId 会话ID
     * @param traceId 跟踪ID
     * @param route 路由类型
     * @param effectiveToolArgs 工具参数
     * @param text 用户输入文本
     * @return 工具执行结果
     */
    private ToolExecutionResult executeAttendanceTool(RequestContext ctx, String sessionId, String traceId, String route, Map<String, Object> effectiveToolArgs, String text) {
        if (effectiveToolArgs != null && slotExtractor.hasRequiredAttendanceSlots(effectiveToolArgs)) {
            String op = effectiveToolArgs != null ? String.valueOf(effectiveToolArgs.getOrDefault("op", "listAnomalies")) : "listAnomalies";
            String toolName = "computeAnomalies".equals(op) ? "attendance.computeAnomalies" : "attendance.listAnomalies";
            long startTime = System.currentTimeMillis();
            ToolResult<Object> toolResult = "computeAnomalies".equals(op)
                    ? attendanceTool.computeAnomalies(sessionId, traceId, effectiveToolArgs)
                    : attendanceTool.listAnomalies(sessionId, traceId, effectiveToolArgs);
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordToolCall(toolName, toolResult.success());
            metricsService.recordToolCallDuration(toolName, duration, toolResult.success());
            return new ToolExecutionResult(toolResult, toolName, null, false, null);
        } else {
            boolean toolArgsMissing = true;
            String followUpPrompt = chatPromptService.buildFollowUpPrompt(route, effectiveToolArgs, "attendance",
                    "employeeId=e-001&start=2026-03-01&end=2026-03-31");
            chatToolArgumentService.cachePending(sessionId, route, effectiveToolArgs, followUpPrompt);
            return new ToolExecutionResult(null, null, null, toolArgsMissing, followUpPrompt);
        }
    }
    
    /**
     * 执行批量考勤查询工具。
     * 
     * @param ctx 请求上下文
     * @param sessionId 会话ID
     * @param traceId 跟踪ID
     * @param route 路由类型
     * @param effectiveToolArgs 工具参数
     * @param text 用户输入文本
     * @return 工具执行结果
     */
    private ToolExecutionResult executeAttendanceMultiSummaryTool(RequestContext ctx, String sessionId, String traceId, String route, Map<String, Object> effectiveToolArgs, String text) {
        if (effectiveToolArgs != null && slotExtractor.hasRequiredAttendanceMultiSummarySlots(effectiveToolArgs)) {
            String toolName = "attendance.listAnomalies(batch)";
            long startTime = System.currentTimeMillis();
            ToolResult<Object> toolResult = aggregateAttendanceMultiSummary(sessionId, traceId, effectiveToolArgs);
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordToolCall(toolName, toolResult.success());
            metricsService.recordToolCallDuration(toolName, duration, toolResult.success());
            return new ToolExecutionResult(toolResult, toolName, null, false, null);
        } else {
            boolean toolArgsMissing = true;
            String followUpPrompt = chatPromptService.buildFollowUpPrompt(route, effectiveToolArgs, "attendance summary",
                    "employeeIds=[e-001,e-002]&start=2026-03-01&end=2026-03-31");
            chatToolArgumentService.cachePending(sessionId, route, effectiveToolArgs, followUpPrompt);
            return new ToolExecutionResult(null, null, null, toolArgsMissing, followUpPrompt);
        }
    }
    
    /**
     * 执行薪资相关工具。
     * 
     * @param ctx 请求上下文
     * @param sessionId 会话ID
     * @param traceId 跟踪ID
     * @param route 路由类型
     * @param effectiveToolArgs 工具参数
     * @param text 用户输入文本
     * @return 工具执行结果
     */
    private ToolExecutionResult executeSalaryTool(RequestContext ctx, String sessionId, String traceId, String route, Map<String, Object> effectiveToolArgs, String text) {
        if (effectiveToolArgs != null && slotExtractor.hasRequiredSalarySlots(effectiveToolArgs)) {
            String toolName = "salary.previewEmployee";
            long startTime = System.currentTimeMillis();
            ToolResult<Object> toolResult = salaryTool.previewEmployee(sessionId, traceId, effectiveToolArgs);
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordToolCall(toolName, toolResult.success());
            metricsService.recordToolCallDuration(toolName, duration, toolResult.success());
            return new ToolExecutionResult(toolResult, toolName, null, false, null);
        } else {
            boolean toolArgsMissing = true;
            String followUpPrompt = chatPromptService.buildFollowUpPrompt(route, effectiveToolArgs, "salary",
                    "employeeId=e-001&payPeriod=2026-03");
            chatToolArgumentService.cachePending(sessionId, route, effectiveToolArgs, followUpPrompt);
            return new ToolExecutionResult(null, null, null, toolArgsMissing, followUpPrompt);
        }
    }
    
    /**
     * 执行薪资比较工具。
     * 
     * @param ctx 请求上下文
     * @param sessionId 会话ID
     * @param traceId 跟踪ID
     * @param route 路由类型
     * @param effectiveToolArgs 工具参数
     * @param text 用户输入文本
     * @return 工具执行结果
     */
    private ToolExecutionResult executeSalaryDiffTool(RequestContext ctx, String sessionId, String traceId, String route, Map<String, Object> effectiveToolArgs, String text) {
        if (effectiveToolArgs != null && slotExtractor.hasRequiredSalarySlots(effectiveToolArgs)) {
            String toolName = "salary.comparePreview";
            long startTime = System.currentTimeMillis();
            ToolResult<Object> toolResult = compareSalaryPreview(sessionId, traceId, effectiveToolArgs);
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordToolCall(toolName, toolResult.success());
            metricsService.recordToolCallDuration(toolName, duration, toolResult.success());
            return new ToolExecutionResult(toolResult, toolName, null, false, null);
        } else {
            boolean toolArgsMissing = true;
            String followUpPrompt = chatPromptService.buildFollowUpPrompt(route, effectiveToolArgs, "salary comparison",
                    "employeeId=e-001&payPeriod=2026-03");
            chatToolArgumentService.cachePending(sessionId, route, effectiveToolArgs, followUpPrompt);
            return new ToolExecutionResult(null, null, null, toolArgsMissing, followUpPrompt);
        }
    }
    
    /**
     * 执行政策相关工具。
     * 
     * @param ctx 请求上下文
     * @param sessionId 会话ID
     * @param traceId 跟踪ID
     * @param route 路由类型
     * @param text 用户输入文本
     * @return 工具执行结果
     */
    private ToolExecutionResult executePolicyTool(RequestContext ctx, String sessionId, String traceId, String route, String text) {
        String toolName = "policy.search";
        long startTime = System.currentTimeMillis();
        Object ragResult = rag.search(ctx.tenantId(), text, 5);
        long duration = System.currentTimeMillis() - startTime;
        metricsService.recordToolCall(toolName, true);
        metricsService.recordToolCallDuration(toolName, duration, true);
        return new ToolExecutionResult(null, null, ragResult, false, null);
    }
    
    /**
     * 执行政策RAG工具。
     * 
     * @param ctx 请求上下文
     * @param text 用户输入文本
     * @return 工具执行结果
     */
    private ToolExecutionResult executePolicyRagTool(RequestContext ctx, String text) {
        String toolName = "policy.rag";
        long startTime = System.currentTimeMillis();
        Object ragResult = rag.search(ctx.tenantId(), text, 5);
        long duration = System.currentTimeMillis() - startTime;
        metricsService.recordToolCall(toolName, true);
        metricsService.recordToolCallDuration(toolName, duration, true);
        return new ToolExecutionResult(null, null, ragResult, false, null);
    }
    
    /**
     * 处理工具执行失败的情况。
     * 
     * @param ctx 请求上下文
     * @param text 用户输入文本
     * @param result 工具执行结果
     * @return 处理后的工具执行结果
     */
    private ToolExecutionResult handleToolExecutionFailure(RequestContext ctx, String text, ToolExecutionResult result) {
        if (result.isToolArgsMissing() || (result.getToolResult() != null && !result.getToolResult().success())) {
            String toolName = "policy.fallback";
            long startTime = System.currentTimeMillis();
            Object ragResult = rag.search(ctx.tenantId(), text, 5);
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordToolCall(toolName, true);
            metricsService.recordToolCallDuration(toolName, duration, true);
            return new ToolExecutionResult(result.getToolResult(), result.getToolName(), ragResult, result.isToolArgsMissing(), result.getFollowUpPrompt());
        }
        return result;
    }
    
    /**
     * 调用LLM生成响应，根据工具执行结果和路由类型构建不同的提示。
     * <p>
     * 该方法根据不同的场景调用LLM：
     * 1. 如果工具执行成功，使用工具结果摘要构建提示
     * 2. 如果是政策推荐场景，使用政策推荐提示
     * 3. 如果是合规风险场景，使用合规风险提示
     * 4. 如果是政策RAG场景，使用政策问答提示
     * 5. 其他场景，直接使用用户输入文本
     * <p>
     * @param ctx 请求上下文，包含租户ID、用户ID等信息
     * @param text 用户输入的文本
     * @param route 路由类型
     * @param toolExecutionResult 工具执行结果
     * @param routeDecision 路由决策
     * @return LLM生成的响应，如果LLM不可用则返回null
     */
    private String callLLM(RequestContext ctx, String text, String route, ToolExecutionResult toolExecutionResult, RouteDecisionDto routeDecision) {
        // 打印 dashscope api-key 配置
        String apiKey = env.getProperty("spring.ai.dashscope.api-key");
        logger.info("spring.ai.dashscope.api-key: {}", apiKey);
        logger.info("spring.ai.dashscope.api-key is blank: {}", apiKey == null || apiKey.isBlank());
        logger.info("spring.ai.dashscope.api-key equals 'sk-placeholder': {}", "sk-placeholder".equals(apiKey));
        
        DashScopeChatService svc = dashScopeChatService.getIfAvailable();
        logger.info("DashScopeChatService is available: {}", svc != null);
        
        if (svc == null) {
            logger.warn("DashScopeChatService is not available. Check if ChatModel bean is registered and spring.ai.dashscope.api-key is configured.");
            return null;
        } else {
            logger.info("DashScopeChatService is available, LLM is enabled.");
        }
        String llmResult = null;
        long startTime = System.currentTimeMillis();
        boolean success = false;
        try {
            ToolResult<Object> toolResult = toolExecutionResult.getToolResult();
            Object ragResult = toolExecutionResult.getRagResult();

            if (toolResult != null && toolResult.success()) {
                String summary = toolResultSummarizer.toSummary(toolResult.data(), route);
                llmResult = svc.chatWithToolContext(text, summary, route);
            } else if ("POLICY_RECOMMEND".equals(route) && ragResult instanceof PolicyRagService.SearchResult sr) {
                llmResult = svc.chat(chatPromptService.buildPolicySuggestionPrompt(text, sr));
            } else if ("COMPLIANCE_RISK".equals(route) && ragResult instanceof PolicyRagService.SearchResult sr) {
                llmResult = svc.chat(chatPromptService.buildCompliancePrompt(text, sr));
            } else if ("POLICY_RAG".equals(route) && ragResult instanceof PolicyRagService.SearchResult sr) {
                llmResult = svc.chat(chatPromptService.buildPolicyQaPrompt(text, sr));
            } else {
                llmResult = svc.chat(text);
            }
            success = true;
        } catch (Exception e) {
            logger.error("LLM call failed: {}", e.getMessage(), e);
            metricsService.recordError("llm_call");
            llmResult = null;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordLlmCall(success);
            metricsService.recordLlmCallDuration(duration, success);
        }
        return llmResult;
    }
    
    private String buildFallbackReason(boolean llmAvailable, boolean toolArgsMissing, ToolResult<Object> toolResult) {
        String fallbackReason = null;
        if (!llmAvailable) {
            if (dashScopeChatService.getIfAvailable() == null) {
                fallbackReason =
                        "LLM 未启用：请检查配置中心 ai-hr-ai-core 中的 spring.ai.dashscope.api-key，并确认运行环境未用空的环境变量覆盖该值。";
            } else {
                fallbackReason = "大模型调用失败或超时，已返回工具/检索结果摘要，可稍后重试。";
            }
        }
        if (toolArgsMissing) {
            fallbackReason = mergeReason(fallbackReason, "Missing required tool arguments. Policy retrieval was used as fallback.");
        }
        if (toolResult != null && !toolResult.success()) {
            fallbackReason = mergeReason(fallbackReason, "Tool call failed: " + toolResult.reason());
        }
        return fallbackReason;
    }
    
    private ToolCallEvidenceDto buildToolCallEvidence(String sessionId, String toolName, ToolResult<Object> toolResult) {
        if (toolResult == null || toolName == null) {
            return null;
        }
        
        String logId = toolCallLogRepo.findFirstBySessionIdOrderByCreatedAtDesc(sessionId)
                .map(log -> log.getId())
                .orElse(null);
        
        if (toolResult.success()) {
            return new ToolCallEvidenceDto(toolName, "SUCCESS", logId, toolResult.data());
        } else {
            return new ToolCallEvidenceDto(toolName, "FAILED", logId, Map.of("reason", toolResult.reason()));
        }
    }

    private String mergeReason(String current, String next) {
        return current == null || current.isBlank() ? next : current + " " + next;
    }


    /**
     * 批量处理多个员工的考勤异常查询，并聚合结果。
     * <p>
     * 该方法使用CompletableFuture并行处理多个员工的考勤查询，提高性能。
     * 主要功能包括：
     * 1. 提取员工ID列表
     * 2. 并行执行每个员工的考勤异常查询
     * 3. 聚合查询结果，包括异常总数、按类型统计、按员工统计等
     * 4. 构建结果摘要和证据
     * <p>
     * @param sessionId 会话ID
     * @param traceId 跟踪ID，用于分布式追踪
     * @param args 工具参数，包含员工ID列表、开始日期和结束日期
     * @return 工具执行结果，包含聚合后的考勤异常信息
     */
    private ToolResult<Object> aggregateAttendanceMultiSummary(String sessionId, String traceId, Map<String, Object> args) {
        String start = String.valueOf(args.get("start"));
        String end = String.valueOf(args.get("end"));
        List<String> employeeIds = extractEmployeeIds(args);
        if (employeeIds.isEmpty()) {
            return ToolResult.failed("employeeIds is empty");
        }

        // 并行处理每个员工的考勤查询
        List<CompletableFuture<EmployeeAttendanceResult>> futures = new ArrayList<>();
        for (String employeeId : employeeIds) {
            CompletableFuture<EmployeeAttendanceResult> future = CompletableFuture.supplyAsync(() -> {
                Map<String, Object> toolArgs = new LinkedHashMap<>();
                toolArgs.put("employeeId", employeeId);
                toolArgs.put("start", start);
                toolArgs.put("end", end);

                ToolResult<Object> res = attendanceTool.listAnomalies(sessionId, traceId, toolArgs);
                return new EmployeeAttendanceResult(employeeId, res);
            }, executorService);
            futures.add(future);
        }

        // 等待所有查询完成
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        List<EmployeeAttendanceResult> results = allOf.thenApply(v ->
                futures.stream()
                        .map(CompletableFuture::join)
                        .toList()
        ).join();

        // 处理结果
        int totalAnomalies = 0;
        Map<String, Integer> overallByType = new LinkedHashMap<>();
        Map<String, Integer> byEmployeeTotals = new LinkedHashMap<>();
        List<Map<String, Object>> topEvidence = new ArrayList<>();
        List<Map<String, Object>> failedEmployees = new ArrayList<>();

        for (EmployeeAttendanceResult result : results) {
            String employeeId = result.getEmployeeId();
            ToolResult<Object> res = result.getToolResult();

            if (!res.success()) {
                failedEmployees.add(Map.of("employeeId", employeeId, "reason", res.reason()));
                continue;
            }

            int employeeTotal = 0;
            if (res.data() instanceof List<?> list) {
                for (Object item : list) {
                    if (!(item instanceof Map<?, ?> anomaly)) {
                        continue;
                    }
                    String type = String.valueOf(anomaly.get("type") != null ? anomaly.get("type") : "");
                    if (!type.isBlank()) {
                        overallByType.merge(type, 1, Integer::sum);
                    }
                    employeeTotal++;
                    totalAnomalies++;
                    if (topEvidence.size() < 10) {
                        Object workDate = anomaly.containsKey("workDate") ? anomaly.get("workDate") : "";
                        Object severity = anomaly.containsKey("severity") ? anomaly.get("severity") : "";
                        topEvidence.add(Map.of(
                                "employeeId", employeeId,
                                "workDate", String.valueOf(workDate),
                                "type", type,
                                "severity", String.valueOf(severity),
                                "message", extractAnomalyMessage(anomaly)
                        ));
                    }
                }
            }
            byEmployeeTotals.put(employeeId, employeeTotal);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("summary", "Analysed " + employeeIds.size() + " employees from " + start + " to " + end
                + " and found " + totalAnomalies + " anomalies.");
        payload.put("start", start);
        payload.put("end", end);
        payload.put("employeeIds", employeeIds);
        payload.put("totalAnomalies", totalAnomalies);
        payload.put("overallByType", overallByType);
        payload.put("byEmployeeTotals", byEmployeeTotals);
        payload.put("failedEmployees", failedEmployees);
        payload.put("topEvidence", topEvidence);

        if (totalAnomalies == 0 && failedEmployees.size() == employeeIds.size()) {
            return ToolResult.failed("all employee queries failed");
        }
        return ToolResult.success(payload);
    }
    
    private static class EmployeeAttendanceResult {
        private final String employeeId;
        private final ToolResult<Object> toolResult;
        
        public EmployeeAttendanceResult(String employeeId, ToolResult<Object> toolResult) {
            this.employeeId = employeeId;
            this.toolResult = toolResult;
        }
        
        public String getEmployeeId() {
            return employeeId;
        }
        
        public ToolResult<Object> getToolResult() {
            return toolResult;
        }
    }

    private List<String> extractEmployeeIds(Map<String, Object> args) {
        Object idsObj = args.get("employeeIds");
        List<String> out = new ArrayList<>();
        if (idsObj instanceof List<?> list) {
            for (Object value : list) {
                if (value != null) {
                    String normalized = String.valueOf(value).trim();
                    if (!normalized.isBlank()) {
                        out.add(normalized);
                    }
                }
            }
        } else if (idsObj != null) {
            String normalized = String.valueOf(idsObj).trim();
            if (!normalized.isBlank()) {
                for (String part : normalized.split("[,;\\s]+")) {
                    if (!part.isBlank()) {
                        out.add(part.trim());
                    }
                }
            }
        }
        if (out.isEmpty() && args.get("employeeId") != null) {
            String single = String.valueOf(args.get("employeeId")).trim();
            if (!single.isBlank()) {
                out.add(single);
            }
        }
        return out;
    }

    private String extractAnomalyMessage(Map<?, ?> anomaly) {
        Object evidenceObj = anomaly.get("evidence");
        if (evidenceObj instanceof Map<?, ?> evidence) {
            Object summary = evidence.get("summary");
            if (summary != null && !String.valueOf(summary).isBlank()) {
                return String.valueOf(summary);
            }
        }
        Object ruleHitObj = anomaly.get("ruleHit");
        if (ruleHitObj instanceof Map<?, ?> ruleHit) {
            Object message = ruleHit.get("message");
            if (message != null && !String.valueOf(message).isBlank()) {
                return String.valueOf(message);
            }
        }
        return "";
    }

    private ToolResult<Object> compareSalaryPreview(String sessionId, String traceId, Map<String, Object> args) {
        Map<String, Object> currentReq = new LinkedHashMap<>(args);
        Map<String, Object> compareReq = new LinkedHashMap<>(args);
        String currentPeriod = String.valueOf(args.get("payPeriod"));
        String comparePeriod = args.get("comparePayPeriod") == null
                ? inferComparePeriod(currentPeriod)
                : String.valueOf(args.get("comparePayPeriod"));
        compareReq.put("payPeriod", comparePeriod);

        ToolResult<Object> current = salaryTool.previewEmployee(sessionId, traceId, currentReq);
        if (!current.success()) {
            return ToolResult.failed("Current period preview failed: " + current.reason());
        }
        ToolResult<Object> compare = salaryTool.previewEmployee(sessionId, traceId, compareReq);
        if (!compare.success()) {
            return ToolResult.failed("Comparison period preview failed: " + compare.reason());
        }

        try {
            return ToolResult.success(buildSalaryDiffPayload(currentPeriod, comparePeriod, current.data(), compare.data()));
        } catch (Exception e) {
            return ToolResult.failed("Failed to build salary diff: " + e.getMessage());
        }
    }

    private String inferComparePeriod(String currentPeriod) {
        try {
            YearMonth ym = YearMonth.parse(currentPeriod, DateTimeFormatter.ofPattern("yyyy-MM"));
            return ym.minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (DateTimeParseException e) {
            return currentPeriod;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildSalaryDiffPayload(String currentPeriod, String comparePeriod, Object currentData, Object compareData) {
        Map<String, Object> current = currentData instanceof Map<?, ?> ? (Map<String, Object>) currentData : Map.of();
        Map<String, Object> compare = compareData instanceof Map<?, ?> ? (Map<String, Object>) compareData : Map.of();
        Map<String, Long> currentLines = lineAmountByCode(current.get("lines"));
        Map<String, Long> compareLines = lineAmountByCode(compare.get("lines"));
        Map<String, Map<String, Object>> diffByCode = new LinkedHashMap<>();

        for (String code : unionCodes(currentLines, compareLines)) {
            long currentAmount = currentLines.getOrDefault(code, 0L);
            long compareAmount = compareLines.getOrDefault(code, 0L);
            diffByCode.put(code, Map.of(
                    "currentAmountCents", currentAmount,
                    "compareAmountCents", compareAmount,
                    "deltaCents", currentAmount - compareAmount
            ));
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("currentPayPeriod", currentPeriod);
        out.put("comparePayPeriod", comparePeriod);
        out.put("employeeId", current.getOrDefault("employeeId", compare.getOrDefault("employeeId", "")));
        out.put("currentResult", current);
        out.put("compareResult", compare);
        out.put("diffByItemCode", diffByCode);
        return out;
    }

    private Map<String, Long> lineAmountByCode(Object linesObj) {
        Map<String, Long> out = new LinkedHashMap<>();
        if (!(linesObj instanceof List<?> list)) {
            return out;
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> line)) {
                continue;
            }
            Object codeObj = line.containsKey("itemCode") ? line.get("itemCode") : "";
            String code = String.valueOf(codeObj);
            if (!code.isBlank()) {
                out.put(code, toLong(line.get("amountCents")));
            }
        }
        return out;
    }

    private List<String> unionCodes(Map<String, Long> current, Map<String, Long> compare) {
        List<String> out = new ArrayList<>(current.keySet());
        for (String code : compare.keySet()) {
            if (!out.contains(code)) {
                out.add(code);
            }
        }
        return out;
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

}



