package com.example.aihr.aicore.web;

import com.example.aihr.aicore.session.SessionContextService;
import com.example.aihr.aicore.slot.SlotExtractor;
import com.example.aihr.common.security.RequestContext;
import com.example.aihr.common.security.RequestContextHolder;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ChatToolArgumentService {
    private final SlotExtractor slotExtractor;
    private final SessionContextService sessionContextService;

    public ChatToolArgumentService(SlotExtractor slotExtractor, SessionContextService sessionContextService) {
        this.slotExtractor = slotExtractor;
        this.sessionContextService = sessionContextService;
    }

    public Map<String, Object> buildEffectiveToolArgs(
            Map<String, Object> requestArgs,
            String message,
            String route,
            String sessionId) {
        if (requestArgs != null && !requestArgs.isEmpty()) {
            Map<String, Object> m = new LinkedHashMap<>(requestArgs);
            fillAttendanceFallbackSlots(m, route);
            return m;
        }
        Map<String, Object> extracted = slotExtractor.extract(message, route);
        SessionContextService.PendingSlots pending = sessionContextService.getAndClear(sessionId);
        Map<String, Object> merged;
        if (pending != null && pending.extractedSoFar() != null) {
            merged = new LinkedHashMap<>(pending.extractedSoFar());
            extracted.forEach(merged::put);
        } else {
            merged = new LinkedHashMap<>(extracted);
        }
        fillAttendanceFallbackSlots(merged, route);
        return merged.isEmpty() ? null : merged;
    }

    /**
     * 考勤类路由在用户未写明工号/日期时，用工号与近30日区间兜底，避免直接走「缺参+RAG」降级。
     */
    private void fillAttendanceFallbackSlots(Map<String, Object> args, String route) {
        if (args == null) return;
        if ("ATTENDANCE".equals(route) || "ATTENDANCE_PATTERN".equals(route)) {
            if (isBlank(args.get("employeeId"))) {
                String fromCtx = inferEmployeeIdFromUser();
                args.put("employeeId", fromCtx != null ? fromCtx : "emp-001");
            }
            if (isBlank(args.get("start")) || isBlank(args.get("end"))) {
                LocalDate end = LocalDate.now();
                LocalDate start = end.minusDays(29);
                args.putIfAbsent("start", start.toString());
                args.putIfAbsent("end", end.toString());
            }
            if (!args.containsKey("op") || isBlank(args.get("op"))) {
                args.put("op", "listAnomalies");
            }
        } else if ("ATTENDANCE_MULTI_SUMMARY".equals(route)) {
            Object idsObj = args.get("employeeIds");
            boolean noIds =
                    !(idsObj instanceof List<?> l && !l.isEmpty())
                            && !(idsObj instanceof String s && !s.isBlank());
            if (noIds && isBlank(args.get("employeeId"))) {
                args.put("employeeIds", List.of("emp-001", "emp-002", "emp-003"));
            } else if (noIds && !isBlank(args.get("employeeId"))) {
                args.put("employeeIds", List.of(String.valueOf(args.get("employeeId"))));
            }
            if (isBlank(args.get("start")) || isBlank(args.get("end"))) {
                LocalDate end = LocalDate.now();
                LocalDate start = end.minusDays(29);
                args.putIfAbsent("start", start.toString());
                args.putIfAbsent("end", end.toString());
            }
            args.putIfAbsent("op", "listAnomalies");
        }
    }

    private static boolean isBlank(Object v) {
        return v == null || String.valueOf(v).isBlank();
    }

    private static String inferEmployeeIdFromUser() {
        RequestContext ctx = RequestContextHolder.getOptional();
        if (ctx == null || ctx.userId() == null || ctx.userId().isBlank()) {
            return null;
        }
        String uid = ctx.userId().trim();
        if (uid.matches("(?i)emp-[\\w-]+|[eE]-\\w+")) {
            return uid;
        }
        return null;
    }

    public void cachePending(String sessionId, String route, Map<String, Object> effectiveToolArgs, String followUpPrompt) {
        if (effectiveToolArgs != null && !effectiveToolArgs.isEmpty()) {
            sessionContextService.setPending(sessionId, route, effectiveToolArgs, followUpPrompt);
        }
    }
}



