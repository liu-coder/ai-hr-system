package com.example.aihr.aicore.web;

import com.example.aihr.aicore.web.dto.RouteDecisionDto;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import com.example.aihr.common.exception.AiHrBusinessException;
import com.example.aihr.common.web.ApiError.ErrorCode;

/**
 * 路由决策：由关键字匹配得出 route + intent 标签与命中词，便于前端展示与后续扩展为意图分类器。
 */
@Service
public class ChatRouteService {

    private static final String[] ATTENDANCE_KEYWORDS = { "考勤", "打卡", "迟到", "缺卡", "早退", "请假", "旷工" };
    private static final String[] SALARY_KEYWORDS = { "薪酬", "工资", "薪资", "个税", "社保", "公积金", "发薪", "工资条" };
    private static final String[] POLICY_RECOMMEND_KEYWORDS = { "推荐规则", "规则建议", "规则优化", "策略建议", "生成规则", "生成策略" };
    private static final String[] ATTENDANCE_SUMMARY_KEYWORDS = { "汇总", "统计", "概览", "总计", "对比", "清单" };
    private static final String[] ATTENDANCE_PATTERN_KEYWORDS = { "原因", "模式", "趋势", "集中", "分析" };
    private static final String[] SALARY_DIFF_KEYWORDS = { "对比", "差异", "变化", "少了", "多了", "环比" };
    private static final String[] COMPLIANCE_KEYWORDS = { "合规", "风险", "最低工资", "社保基数", "是否符合", "是否合法" };

    // 消歧：制度/规定类问题更倾向政策检索（而不是直接调工具）。
    private static final String[] POLICY_LIKE_KEYWORDS = { "制度", "规定", "政策", "办法", "细则" };
    
    private final InputValidator inputValidator;
    
    public ChatRouteService(InputValidator inputValidator) {
        this.inputValidator = inputValidator;
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

        if (containsAny(t, POLICY_RECOMMEND_KEYWORDS)) {
            return new RouteDecisionDto("POLICY_RECOMMEND", "规则/策略建议", matchKeywords(t, POLICY_RECOMMEND_KEYWORDS));
        }

        if (containsAny(t, COMPLIANCE_KEYWORDS)) {
            return new RouteDecisionDto("COMPLIANCE_RISK", "合规风险提示", matchKeywords(t, COMPLIANCE_KEYWORDS));
        }

        if (containsAny(t, ATTENDANCE_KEYWORDS) && containsAny(t, ATTENDANCE_PATTERN_KEYWORDS)) {
            List<String> matched = new ArrayList<>(matchKeywords(t, ATTENDANCE_KEYWORDS));
            matched.addAll(matchKeywords(t, ATTENDANCE_PATTERN_KEYWORDS));
            return new RouteDecisionDto("ATTENDANCE_PATTERN", "考勤模式分析", matched);
        }

        if (containsAny(t, ATTENDANCE_KEYWORDS) && containsAny(t, ATTENDANCE_SUMMARY_KEYWORDS)) {
            List<String> matched = new ArrayList<>(matchKeywords(t, ATTENDANCE_SUMMARY_KEYWORDS));
            return new RouteDecisionDto("ATTENDANCE_MULTI_SUMMARY", "考勤多员工汇总", matched);
        }

        if (containsAny(t, SALARY_KEYWORDS) && containsAny(t, SALARY_DIFF_KEYWORDS)) {
            List<String> matched = new ArrayList<>(matchKeywords(t, SALARY_KEYWORDS));
            matched.addAll(matchKeywords(t, SALARY_DIFF_KEYWORDS));
            return new RouteDecisionDto("SALARY_DIFF", "薪资差异解释", matched);
        }

        // 1) 消歧：如果包含制度/规定，并且同时包含考勤/薪酬相关词 -> 优先 POLICY_RAG
        boolean policyLike = false;
        for (String kw : POLICY_LIKE_KEYWORDS) {
            if (t.contains(kw)) {
                policyLike = true;
                break;
            }
        }
        boolean hasAttendance = containsAny(t, ATTENDANCE_KEYWORDS);
        boolean hasSalary = containsAny(t, SALARY_KEYWORDS);

        if (policyLike && (hasAttendance || hasSalary)) {
            List<String> matched = new ArrayList<>();
            for (String kw : POLICY_LIKE_KEYWORDS) if (t.contains(kw)) matched.add(kw);
            if (hasAttendance) matched.add("考勤相关");
            if (hasSalary) matched.add("薪酬相关");
            return new RouteDecisionDto("POLICY_RAG", "制度/规定检索", List.copyOf(matched));
        }

        // 2) 加权关键词打分（避免单纯命中导致误路由）
        int attendanceScore = score(t, ATTENDANCE_KEYWORDS, 2);
        int salaryScore = score(t, SALARY_KEYWORDS, 2);

        if (attendanceScore == 0 && salaryScore == 0) {
            return new RouteDecisionDto("POLICY_RAG", "政策检索", List.of());
        }

        if (attendanceScore >= salaryScore) {
            return new RouteDecisionDto("ATTENDANCE", "考勤查询", matchKeywords(t, ATTENDANCE_KEYWORDS));
        }
        return new RouteDecisionDto("SALARY", "薪酬查询", matchKeywords(t, SALARY_KEYWORDS));
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
