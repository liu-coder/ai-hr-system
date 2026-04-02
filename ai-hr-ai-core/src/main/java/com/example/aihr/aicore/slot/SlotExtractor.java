package com.example.aihr.aicore.slot;

import java.time.LocalDate;
import java.time.YearMonth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 从自然语言中抽取考勤/薪酬工具所需槽位，用于自动组 toolArgs（扩展点：自然语言查考勤/查薪资）。
 * 当前为规则+正则实现，后续可替换为 NER 或小模型。
 */
@Component
public class SlotExtractor {

    private static final Pattern ISO_DATE = Pattern.compile("(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})");
    private static final Pattern YEAR_MONTH = Pattern.compile("(\\d{4})[-/年]?(\\d{1,2})[月份]?|(\\d{1,2})月");
    private static final Pattern EMPLOYEE_ID = Pattern.compile("(?:员工)?[IDid]?[:：]?\\s*([eE]-\\w+|\\d{3,})|工号[:：]?\\s*([eE]-\\w+|\\d{3,})");
    private static final Pattern NAME_HINT = Pattern.compile("([\\u4e00-\\u9fa5]{2,6})的(考勤|工资|薪酬|迟到|缺卡|早退)");

    /**
     * 按路由从文本中抽取槽位并组装为 toolArgs；缺失的键不放入，由调用方判断是否追问。
     *
     * @param message 用户消息
     * @param route   ATTENDANCE | SALARY
     * @return 可能包含 employeeId, start, end（考勤）或 employeeId, payPeriod（薪酬）及 op（考勤时）
     */
    public Map<String, Object> extract(String message, String route) {
        if (message == null || message.isBlank()) return Map.of();
        String text = message.trim();
        Map<String, Object> out = new HashMap<>();

        if ("ATTENDANCE".equals(route) || "ATTENDANCE_PATTERN".equals(route)) {
            extractAttendanceSlots(text, out);
        } else if ("ATTENDANCE_MULTI_SUMMARY".equals(route)) {
            extractAttendanceSlots(text, out);
            // 多员工汇总只需要查询列表结果，不需要 compute。
            out.put("op", "listAnomalies");
            List<String> employeeIds = extractEmployeeIds(text);
            if (employeeIds != null && !employeeIds.isEmpty()) {
                out.put("employeeIds", employeeIds);
            } else if (out.get("employeeId") != null) {
                // 兼容：如果只识别到单个 employeeId，也能当作单元素列表处理。
                out.put("employeeIds", List.of(String.valueOf(out.get("employeeId"))));
            }
        } else if ("SALARY".equals(route) || "SALARY_DIFF".equals(route)) {
            extractSalarySlots(text, out);
            if ("SALARY_DIFF".equals(route)) {
                String comparePayPeriod = extractComparePayPeriod(text, (String) out.get("payPeriod"));
                if (comparePayPeriod != null) out.put("comparePayPeriod", comparePayPeriod);
            }
        }
        return out;
    }

    private void extractAttendanceSlots(String text, Map<String, Object> out) {
        LocalDate[] range = extractDateRange(text);
        if (range != null) {
            out.put("start", range[0].toString());
            out.put("end", range[1].toString());
        }
        String emp = extractEmployeeId(text);
        if (emp != null) out.put("employeeId", emp);
        String nameHint = extractNameHint(text);
        if (nameHint != null) out.put("nameHint", nameHint);
        if (text.contains("计算") || text.contains("算一下") || text.contains("跑一下")) {
            out.put("op", "computeAnomalies");
        } else {
            out.put("op", "listAnomalies");
        }
    }

    private void extractSalarySlots(String text, Map<String, Object> out) {
        String payPeriod = extractPayPeriod(text);
        if (payPeriod != null) out.put("payPeriod", payPeriod);
        String emp = extractEmployeeId(text);
        if (emp != null) out.put("employeeId", emp);
        String nameHint = extractNameHint(text);
        if (nameHint != null) out.put("nameHint", nameHint);
        out.put("input", Map.of());
    }

    private String extractEmployeeId(String text) {
        Matcher m = EMPLOYEE_ID.matcher(text);
        if (m.find()) {
            for (int i = 1; i <= m.groupCount(); i++) {
                if (m.group(i) != null && !m.group(i).isBlank()) return m.group(i).trim();
            }
        }
        return null;
    }

    private String extractPayPeriod(String text) {
        // 2026-03 / 2026年3月 / 3月
        Matcher ym = Pattern.compile("(\\d{4})[-年]?(\\d{1,2})月?").matcher(text);
        if (ym.find()) {
            int y = Integer.parseInt(ym.group(1));
            int mo = Integer.parseInt(ym.group(2));
            return String.format("%d-%02d", y, mo);
        }
        Matcher mOnly = Pattern.compile("(\\d{1,2})月").matcher(text);
        if (mOnly.find()) {
            int mo = Integer.parseInt(mOnly.group(1));
            int y = LocalDate.now().getYear();
            return String.format("%d-%02d", y, mo);
        }
        return null;
    }

    private String extractComparePayPeriod(String text, String currentPayPeriod) {
        if (currentPayPeriod == null || currentPayPeriod.isBlank()) return null;
        try {
            YearMonth current = YearMonth.parse(currentPayPeriod);
            if (text.contains("上月")) return current.minusMonths(1).toString();
            if (text.contains("上上月")) return current.minusMonths(2).toString();
            if (text.contains("去年同月")) return current.minusYears(1).toString();
        } catch (Exception ignored) { }
        Matcher ym = Pattern.compile("对比\\s*(\\d{4})[-年]?(\\d{1,2})月?").matcher(text);
        if (ym.find()) {
            int y = Integer.parseInt(ym.group(1));
            int mo = Integer.parseInt(ym.group(2));
            if (mo >= 1 && mo <= 12) return String.format("%d-%02d", y, mo);
        }
        return null;
    }

    private LocalDate[] extractDateRange(String text) {
        // 口语化日期：今天/本周
        if (text.contains("今天")) {
            LocalDate d = LocalDate.now();
            return new LocalDate[]{d, d};
        }
        if (text.contains("30天") || text.contains("30 天") || text.contains("近30") || text.contains("三十天")
                || text.contains("一个月") || text.contains("近一月")) {
            LocalDate end = LocalDate.now();
            return new LocalDate[]{end.minusDays(29), end};
        }
        if (text.contains("本周")) {
            LocalDate now = LocalDate.now();
            // 周一作为起点
            LocalDate start = now.minusDays((now.getDayOfWeek().getValue() - 1));
            LocalDate end = start.plusDays(6);
            return new LocalDate[]{start, end};
        }

        java.util.List<LocalDate> dates = new java.util.ArrayList<>();
        Matcher m = ISO_DATE.matcher(text);
        while (m.find()) {
            try {
                dates.add(LocalDate.of(
                        Integer.parseInt(m.group(1)),
                        Integer.parseInt(m.group(2)),
                        Integer.parseInt(m.group(3))));
            } catch (Exception ignored) { }
        }
        if (dates.size() >= 2) {
            LocalDate a = dates.get(0);
            LocalDate b = dates.get(1);
            return new LocalDate[]{a.isBefore(b) ? a : b, a.isBefore(b) ? b : a};
        }
        if (dates.size() == 1) {
            LocalDate d = dates.get(0);
            LocalDate start = d.withDayOfMonth(1);
            LocalDate end = d.withDayOfMonth(d.lengthOfMonth());
            return new LocalDate[]{start, end};
        }
        YearMonth ym = extractYearMonthFromChinese(text);
        if (ym != null) {
            return new LocalDate[]{ym.atDay(1), ym.atEndOfMonth()};
        }
        return null;
    }

    private YearMonth extractYearMonthFromChinese(String text) {
        int year = LocalDate.now().getYear();
        Integer month = null;
        if (text.contains("本月")) month = LocalDate.now().getMonthValue();
        else if (text.contains("上月")) {
            LocalDate prev = LocalDate.now().minusMonths(1);
            year = prev.getYear();
            month = prev.getMonthValue();
        } else {
            Matcher m = YEAR_MONTH.matcher(text);
            if (m.find()) {
                if (m.group(1) != null) year = Integer.parseInt(m.group(1));
                if (m.group(2) != null) month = Integer.parseInt(m.group(2));
                else if (m.group(3) != null) month = Integer.parseInt(m.group(3));
            }
        }
        if (month != null && month >= 1 && month <= 12) {
            return YearMonth.of(year, month);
        }
        return null;
    }

    private String extractNameHint(String text) {
        Matcher m = NAME_HINT.matcher(text);
        if (m.find()) {
            String name = m.group(1);
            if (name != null && !name.isBlank()) return name.trim();
        }
        return null;
    }

    /**
     * 判断考勤工具是否已具备必填槽位：employeeId, start, end。
     */
    public boolean hasRequiredAttendanceSlots(Map<String, Object> args) {
        return args != null
                && args.get("employeeId") != null && !String.valueOf(args.get("employeeId")).isBlank()
                && args.get("start") != null && !String.valueOf(args.get("start")).isBlank()
                && args.get("end") != null && !String.valueOf(args.get("end")).isBlank();
    }

    /**
     * 判断考勤多员工汇总是否已具备必填槽位：employeeIds, start, end。
     */
    public boolean hasRequiredAttendanceMultiSummarySlots(Map<String, Object> args) {
        if (args == null) return false;
        Object idsObj = args.get("employeeIds");
        boolean hasIds = false;
        if (idsObj instanceof List<?> l) {
            hasIds = !l.isEmpty() && l.stream().anyMatch(v -> v != null && !String.valueOf(v).isBlank());
        } else if (idsObj instanceof String s) {
            hasIds = !s.isBlank();
        }
        return hasIds
                && args.get("start") != null && !String.valueOf(args.get("start")).isBlank()
                && args.get("end") != null && !String.valueOf(args.get("end")).isBlank();
    }

    /**
     * 在消息中抽取所有可能的 employeeId（去重保序）。
     */
    private List<String> extractEmployeeIds(String text) {
        if (text == null || text.isBlank()) return List.of();
        Matcher m = EMPLOYEE_ID.matcher(text);
        LinkedHashSet<String> set = new LinkedHashSet<>();
        while (m.find()) {
            for (int i = 1; i <= m.groupCount(); i++) {
                String g = m.group(i);
                if (g != null && !g.isBlank()) {
                    set.add(g.trim());
                }
            }
        }
        return new ArrayList<>(set);
    }

    /**
     * 判断薪酬预览是否已具备必填槽位：employeeId, payPeriod。
     */
    public boolean hasRequiredSalarySlots(Map<String, Object> args) {
        return args != null
                && args.get("employeeId") != null && !String.valueOf(args.get("employeeId")).isBlank()
                && args.get("payPeriod") != null && !String.valueOf(args.get("payPeriod")).isBlank();
    }
}



