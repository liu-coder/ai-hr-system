package com.example.aihr.aicore.llm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

/**
 * 基于自动配置的 ChatModel 创建 ChatClient。
 */
@Service
public class DashScopeChatService {
    private final ChatClient chatClient;

    public DashScopeChatService(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    public String chat(String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    /**
     * 带工具结果的对话：将考勤/薪酬工具返回的结构化结果作为上下文，生成解读与建议（扩展点：异常解读、薪资解读）。
     *
     * @param userMessage      用户原始问题
     * @param toolResultSummary 工具返回数据的文本摘要（如异常条数、明细摘要、薪资 lines 摘要）
     * @param domain            ATTENDANCE | SALARY，用于提示模型角色
     */
    public String chatWithToolContext(String userMessage, String toolResultSummary, String domain) {
        String domainLabel =
                "ATTENDANCE".equals(domain) ? "考勤异常"
                        : "ATTENDANCE_PATTERN".equals(domain) ? "考勤模式分析"
                        : "ATTENDANCE_MULTI_SUMMARY".equals(domain) ? "考勤汇总"
                        : "SALARY".equals(domain) ? "薪酬预览"
                        : "SALARY_DIFF".equals(domain) ? "薪资差异"
                        : "业务数据";
        String prompt = "用户问题：" + userMessage + "\n\n上述查询的" + domainLabel + "结果如下：\n" + toolResultSummary
                + "\n请用一两段简短自然语言总结上述结果，并酌情给出改进建议或说明。";
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}
