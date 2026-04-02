package com.example.aihr.aicore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 聊天路由配置类
 */
@Component
@ConfigurationProperties(prefix = "aihr.chat.route")
public class ChatRouteConfig {
    
    private String[] attendanceKeywords = { "考勤", "打卡", "迟到", "缺卡", "早退", "请假", "旷工" };
    private String[] salaryKeywords = { "薪酬", "工资", "薪资", "个税", "社保", "公积金", "发薪", "工资条" };
    private String[] policyRecommendKeywords = { "推荐规则", "规则建议", "规则优化", "策略建议", "生成规则", "生成策略" };
    private String[] attendanceSummaryKeywords = { "汇总", "统计", "概览", "总计", "对比", "清单" };
    private String[] attendancePatternKeywords = { "原因", "模式", "趋势", "集中", "分析" };
    private String[] salaryDiffKeywords = { "对比", "差异", "变化", "少了", "多了", "环比" };
    private String[] complianceKeywords = { "合规", "风险", "最低工资", "社保基数", "是否符合", "是否合法" };
    private String[] policyLikeKeywords = { "制度", "规定", "政策", "办法", "细则" };
    
    // Getters and setters
    public String[] getAttendanceKeywords() {
        return attendanceKeywords;
    }
    public void setAttendanceKeywords(String[] attendanceKeywords) {
        this.attendanceKeywords = attendanceKeywords;
    }
    public String[] getSalaryKeywords() {
        return salaryKeywords;
    }
    public void setSalaryKeywords(String[] salaryKeywords) {
        this.salaryKeywords = salaryKeywords;
    }
    public String[] getPolicyRecommendKeywords() {
        return policyRecommendKeywords;
    }
    public void setPolicyRecommendKeywords(String[] policyRecommendKeywords) {
        this.policyRecommendKeywords = policyRecommendKeywords;
    }
    public String[] getAttendanceSummaryKeywords() {
        return attendanceSummaryKeywords;
    }
    public void setAttendanceSummaryKeywords(String[] attendanceSummaryKeywords) {
        this.attendanceSummaryKeywords = attendanceSummaryKeywords;
    }
    public String[] getAttendancePatternKeywords() {
        return attendancePatternKeywords;
    }
    public void setAttendancePatternKeywords(String[] attendancePatternKeywords) {
        this.attendancePatternKeywords = attendancePatternKeywords;
    }
    public String[] getSalaryDiffKeywords() {
        return salaryDiffKeywords;
    }
    public void setSalaryDiffKeywords(String[] salaryDiffKeywords) {
        this.salaryDiffKeywords = salaryDiffKeywords;
    }
    public String[] getComplianceKeywords() {
        return complianceKeywords;
    }
    public void setComplianceKeywords(String[] complianceKeywords) {
        this.complianceKeywords = complianceKeywords;
    }
    public String[] getPolicyLikeKeywords() {
        return policyLikeKeywords;
    }
    public void setPolicyLikeKeywords(String[] policyLikeKeywords) {
        this.policyLikeKeywords = policyLikeKeywords;
    }
}