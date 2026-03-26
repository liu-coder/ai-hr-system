package com.example.aihr.aicore.web;

import com.example.aihr.aicore.rag.PolicyRagService;
import com.example.aihr.aicore.web.dto.RouteDecisionDto;
import com.example.aihr.aicore.web.dto.ToolCallEvidenceDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ChatPromptService {
    public String buildFollowUpPrompt(String route, Map<String, Object> extracted, String domainLabel, String example) {
        if (extracted == null) {
            return "Please provide the missing " + domainLabel + " parameters. Example: " + example;
        }
        List<String> missing = new ArrayList<>();
        if ("ATTENDANCE".equals(route) || "ATTENDANCE_PATTERN".equals(route)) {
            addIfMissing(missing, extracted, "employeeId");
            addIfMissing(missing, extracted, "start");
            addIfMissing(missing, extracted, "end");
        } else if ("ATTENDANCE_MULTI_SUMMARY".equals(route)) {
            if (!hasEmployeeIds(extracted.get("employeeIds"))) {
                missing.add("employeeIds");
            }
            addIfMissing(missing, extracted, "start");
            addIfMissing(missing, extracted, "end");
        } else if ("SALARY".equals(route) || "SALARY_DIFF".equals(route)) {
            addIfMissing(missing, extracted, "employeeId");
            addIfMissing(missing, extracted, "payPeriod");
        }
        return missing.isEmpty() ? null : "Missing " + String.join(", ", missing) + ". Example: " + example;
    }

    public String buildEvidenceSummary(RouteDecisionDto route, ToolCallEvidenceDto toolCall, Object ragResult,
                                       boolean llmAvailable, boolean toolArgsMissing) {
        StringBuilder sb = new StringBuilder();
        if (toolCall != null) {
            sb.append("Executed tool for ").append(route.intentLabel()).append(".");
        } else if (toolArgsMissing) {
            sb.append("Intent detected for ").append(route.intentLabel()).append(", but required tool arguments were missing.");
        }
        if (ragResult instanceof PolicyRagService.SearchResult sr && sr.hits() != null) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append("Policy retrieval returned ").append(sr.hits().size()).append(" hits.");
        }
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(llmAvailable ? "Generated an LLM response." : "Returned deterministic fallback content.");
        return sb.toString();
    }

    public String buildPolicySuggestionPrompt(String userMessage, PolicyRagService.SearchResult sr) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an HR policy advisor.\nUser request: ").append(userMessage).append("\nRelevant policy excerpts:\n");
        sr.hits().stream().limit(5).forEach(hit -> sb.append("- ").append(hit.content()).append('\n'));
        sb.append("Provide actionable rule suggestions, an example JSON rule draft, and key risks.");
        return sb.toString();
    }

    public String buildPolicyQaPrompt(String userMessage, PolicyRagService.SearchResult sr) {
        StringBuilder sb = new StringBuilder();
        sb.append("Answer the HR policy question strictly from the retrieved excerpts.\nUser request: ")
                .append(userMessage).append("\n");
        sr.hits().stream().limit(5).forEach(hit -> sb.append("- [chunk=").append(hit.chunkIndex()).append("] ")
                .append(hit.content()).append('\n'));
        sb.append("Answer directly, cite the evidence, and say when the excerpts are insufficient.");
        return sb.toString();
    }

    public String buildCompliancePrompt(String userMessage, PolicyRagService.SearchResult sr) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an HR compliance assistant.\nUser request: ").append(userMessage).append("\nPolicy excerpts:\n");
        sr.hits().stream().limit(5).forEach(hit -> sb.append("- ").append(hit.content()).append('\n'));
        sb.append("Return 3-5 items in risk-cause-action format without giving legal conclusions.");
        return sb.toString();
    }

    private void addIfMissing(List<String> missing, Map<String, Object> extracted, String key) {
        Object value = extracted.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            missing.add(key);
        }
    }

    private boolean hasEmployeeIds(Object idsObj) {
        if (idsObj instanceof List<?> list) {
            return !list.isEmpty() && list.stream().anyMatch(v -> v != null && !String.valueOf(v).isBlank());
        }
        return idsObj != null && !String.valueOf(idsObj).isBlank();
    }
}
