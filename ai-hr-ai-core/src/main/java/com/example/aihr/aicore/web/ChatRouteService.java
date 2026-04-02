package com.example.aihr.aicore.web;

import com.example.aihr.aicore.config.ChatRouteConfig;
import com.example.aihr.aicore.web.dto.RouteDecisionDto;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import com.example.aihr.common.exception.AiHrBusinessException;
import com.example.aihr.common.validation.InputValidator;
import com.example.aihr.common.web.ApiError.ErrorCode;

/**
 * 路由决策：由关键字匹配得出 route + intent 标签与命中词，便于前端展示与后续扩展为意图分类器。
 */
@Service
public class ChatRouteService {

    private final InputValidator inputValidator;
    private final ChatRouteConfig chatRouteConfig;
    
    public ChatRouteService(InputValidator inputValidator, ChatRouteConfig chatRouteConfig) {
        this.inputValidator = inputValidator;
        this.chatRouteConfig = chatRouteConfig;
    }

    public RouteDecisionDto decide(String text) {
        // P0: 输入验证
        if (text == null || text.isBlank()) {
            throw new AiHrBusinessException(ErrorCode.PARAM_INVALID_ARGUMENT, "消息内容不能为空");
        }
        
        // 长度限制
        inputValidator.validateLength(text, 2000, "消息内容");
        
        // XSS 和注入防护
        if (!inputValidator.isValidUserInput(text)) {
            throw new AiHrBusinessException(ErrorCode.PARAM_INVALID_ARGUMENT, "输入包含非法字符");
        }
        
        String t = text.trim().toLowerCase();

        if (containsAny(t, chatRouteConfig.getPolicyRecommendKeywords())) {
            return new RouteDecisionDto("POLICY_RECOMMEND", "规则/策略建议", matchKeywords(t, chatRouteConfig.getPolicyRecommendKeywords()));
        }

        if (containsAny(t, chatRouteConfig.getComplianceKeywords())) {
            return new RouteDecisionDto("COMPLIANCE_RISK", "合规风险提示", matchKeywords(t, chatRouteConfig.getComplianceKeywords()));
        }

        if (containsAny(t, chatRouteConfig.getAttendanceKeywords()) && containsAny(t, chatRouteConfig.getAttendancePatternKeywords())) {
            List<String> matched = new ArrayList<>(matchKeywords(t, chatRouteConfig.getAttendanceKeywords()));
            matched.addAll(matchKeywords(t, chatRouteConfig.getAttendancePatternKeywords()));
            return new RouteDecisionDto("ATTENDANCE_PATTERN", "考勤模式分析", matched);
        }

        if (containsAny(t, chatRouteConfig.getAttendanceKeywords()) && containsAny(t, chatRouteConfig.getAttendanceSummaryKeywords())) {
            List<String> matched = new ArrayList<>(matchKeywords(t, chatRouteConfig.getAttendanceSummaryKeywords()));
            return new RouteDecisionDto("ATTENDANCE_MULTI_SUMMARY", "考勤多员工汇总", matched);
        }

        if (containsAny(t, chatRouteConfig.getSalaryKeywords()) && containsAny(t, chatRouteConfig.getSalaryDiffKeywords())) {
            List<String> matched = new ArrayList<>(matchKeywords(t, chatRouteConfig.getSalaryKeywords()));
            matched.addAll(matchKeywords(t, chatRouteConfig.getSalaryDiffKeywords()));
            return new RouteDecisionDto("SALARY_DIFF", "薪资差异解释", matched);
        }

        // 1) 消歧：如果包含制度/规定，并且同时包含考勤/薪酬相关词 -> 优先 POLICY_RAG
        boolean policyLike = false;
        for (String kw : chatRouteConfig.getPolicyLikeKeywords()) {
            if (t.contains(kw)) {
                policyLike = true;
                break;
            }
        }
        boolean hasAttendance = containsAny(t, chatRouteConfig.getAttendanceKeywords());
        boolean hasSalary = containsAny(t, chatRouteConfig.getSalaryKeywords());

        if (policyLike && (hasAttendance || hasSalary)) {
            List<String> matched = new ArrayList<>();
            for (String kw : chatRouteConfig.getPolicyLikeKeywords()) if (t.contains(kw)) matched.add(kw);
            if (hasAttendance) matched.add("考勤相关");
            if (hasSalary) matched.add("薪酬相关");
            return new RouteDecisionDto("POLICY_RAG", "制度/规定检索", List.copyOf(matched));
        }

        // 2) 加权关键词打分（避免单纯命中导致误路由）
        int attendanceScore = score(t, chatRouteConfig.getAttendanceKeywords(), 2);
        int salaryScore = score(t, chatRouteConfig.getSalaryKeywords(), 2);

        if (attendanceScore == 0 && salaryScore == 0) {
            return new RouteDecisionDto("POLICY_RAG", "政策检索", List.of());
        }

        if (attendanceScore >= salaryScore) {
            return new RouteDecisionDto("ATTENDANCE", "考勤查询", matchKeywords(t, chatRouteConfig.getAttendanceKeywords()));
        }
        return new RouteDecisionDto("SALARY", "薪酬查询", matchKeywords(t, chatRouteConfig.getSalaryKeywords()));
    }

    private static boolean containsAny(String text, String[] kws) {
        for (String kw : kws) if (text.contains(kw)) return true;
        return false;
    }

    private static int score(String text, String[] kws, int weight) {
        int s = 0;
        for (String kw : kws) {
            if (text.contains(kw)) s += weight;
        }
        return s;
    }

    private static List<String> matchKeywords(String text, String[] kws) {
        List<String> out = new ArrayList<>();
        for (String kw : kws) if (text.contains(kw)) out.add(kw);
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }
}



