package com.example.aihr.aicore.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 将工具返回的考勤/薪酬结果转为简短文本摘要，供 LLM 做解读与建议（扩展点：异常解读、薪资解读）。
 */
@Component
public class ToolResultSummarizer {

    private final ObjectMapper om = new ObjectMapper();

    /**
     * @param toolResult 考勤异常列表或薪酬预览 Map（与域接口返回结构一致）
     * @param domain     ATTENDANCE | SALARY
     */
    public String toSummary(Object toolResult, String domain) {
        if (toolResult == null) return "（无数据）";
        if ("ATTENDANCE".equals(domain)) return summarizeAttendance(toolResult);
        if ("ATTENDANCE_PATTERN".equals(domain)) return summarizeAttendance(toolResult);
        if ("ATTENDANCE_MULTI_SUMMARY".equals(domain)) return summarizeAttendanceMultiSummary(toolResult);
        if ("SALARY".equals(domain)) return summarizeSalary(toolResult);
        if ("SALARY_DIFF".equals(domain)) return summarizeSalaryDiff(toolResult);
        return fallbackJson(toolResult);
    }

    private String summarizeAttendance(Object toolResult) {
        try {
            JsonNode arr = om.valueToTree(toolResult);
            if (arr.isObject() && arr.has("data") && arr.get("data").isArray()) {
                arr = arr.get("data");
            }
            if (!arr.isArray()) return fallbackJson(toolResult);
            int n = arr.size();
            if (n == 0) return "该时间段内无考勤异常记录。";
            List<String> parts = new ArrayList<>();
            Map<String, Integer> byType = new java.util.HashMap<>();
            for (JsonNode item : arr) {
                String type = item.has("type") ? item.get("type").asText("") : "";
                byType.merge(type, 1, Integer::sum);
                if (item.has("workDate") && item.has("ruleHit")) {
                    String msg = item.has("evidence") && item.get("evidence").has("summary")
                            ? item.get("evidence").get("summary").asText("")
                            : ruleHitMessage(item.get("ruleHit"));
                    parts.add(item.get("workDate").asText("") + " " + msg);
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append("共 ").append(n).append(" 条异常。");
            if (!byType.isEmpty()) {
                byType.forEach((t, c) -> sb.append(" ").append(t).append(" ").append(c).append(" 次；"));
            }
            sb.append("\n明细：");
            parts.stream().limit(10).forEach(p -> sb.append("\n- ").append(p));
            if (parts.size() > 10) sb.append("\n... 等共 ").append(parts.size()).append(" 条");
            return sb.toString();
        } catch (Exception e) {
            return fallbackJson(toolResult);
        }
    }

    private String summarizeSalary(Object toolResult) {
        try {
            JsonNode root = om.valueToTree(toolResult);
            if (!root.isObject()) return fallbackJson(toolResult);
            StringBuilder sb = new StringBuilder();
            if (root.has("lines") && root.get("lines").isArray()) {
                long net = 0;
                List<String> lineTexts = new ArrayList<>();
                for (JsonNode line : root.get("lines")) {
                    String code = line.has("itemCode") ? line.get("itemCode").asText("") : "";
                    String name = line.has("itemName") ? line.get("itemName").asText("") : code;
                    long amount = line.has("amountCents") ? line.get("amountCents").asLong(0) : 0;
                    if ("NET".equals(code)) net = amount;
                    lineTexts.add(name + "：" + (amount / 100.0) + " 元");
                }
                sb.append("明细：").append(String.join("；", lineTexts)).append("。");
                if (net != 0) sb.append(" 实发合计：").append(net / 100.0).append(" 元。");
            } else {
                sb.append(fallbackJson(toolResult));
            }
            return sb.toString();
        } catch (Exception e) {
            return fallbackJson(toolResult);
        }
    }

    private static String ruleHitMessage(JsonNode ruleHit) {
        if (ruleHit == null || ruleHit.isNull()) {
            return "";
        }
        if (ruleHit.isTextual()) {
            return ruleHit.asText("");
        }
        if (ruleHit.isObject() && ruleHit.has("message")) {
            return ruleHit.get("message").asText("");
        }
        return ruleHit.toString();
    }

    private String fallbackJson(Object o) {
        try {
            return om.writeValueAsString(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }

    private String summarizeAttendanceMultiSummary(Object toolResult) {
        try {
            JsonNode root = om.valueToTree(toolResult);
            if (root.isObject() && root.has("summary")) {
                return root.get("summary").asText("");
            }
            return fallbackJson(toolResult);
        } catch (Exception e) {
            return fallbackJson(toolResult);
        }
    }

    private String summarizeSalaryDiff(Object toolResult) {
        try {
            JsonNode root = om.valueToTree(toolResult);
            if (!root.isObject()) return fallbackJson(toolResult);
            String cur = root.path("currentPayPeriod").asText("");
            String cmp = root.path("comparePayPeriod").asText("");
            JsonNode diff = root.path("diffByItemCode");
            if (!diff.isObject()) return fallbackJson(toolResult);
            StringBuilder sb = new StringBuilder();
            sb.append("薪资对比：").append(cur).append(" vs ").append(cmp).append("。");
            int count = 0;
            var it = diff.fields();
            while (it.hasNext()) {
                var entry = it.next();
                JsonNode d = entry.getValue();
                long delta = d.path("deltaCents").asLong(0);
                if (delta == 0) continue;
                sb.append(" ").append(entry.getKey()).append(delta > 0 ? "增加" : "减少")
                        .append(Math.abs(delta) / 100.0).append("元；");
                count++;
                if (count >= 8) break;
            }
            if (count == 0) {
                sb.append("主要项目无明显变化。");
            }
            return sb.toString();
        } catch (Exception e) {
            return fallbackJson(toolResult);
        }
    }
}



