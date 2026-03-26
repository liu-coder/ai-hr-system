package com.example.aihr.aicore.web;

import com.example.aihr.aicore.session.SessionContextService;
import com.example.aihr.aicore.slot.SlotExtractor;
import java.util.LinkedHashMap;
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
            return new LinkedHashMap<>(requestArgs);
        }
        Map<String, Object> extracted = slotExtractor.extract(message, route);
        SessionContextService.PendingSlots pending = sessionContextService.getAndClear(sessionId);
        if (pending != null && pending.extractedSoFar() != null) {
            Map<String, Object> merged = new LinkedHashMap<>(pending.extractedSoFar());
            extracted.forEach(merged::put);
            return merged;
        }
        return extracted.isEmpty() ? null : extracted;
    }

    public void cachePending(String sessionId, String route, Map<String, Object> effectiveToolArgs, String followUpPrompt) {
        if (effectiveToolArgs != null && !effectiveToolArgs.isEmpty()) {
            sessionContextService.setPending(sessionId, route, effectiveToolArgs, followUpPrompt);
        }
    }
}
