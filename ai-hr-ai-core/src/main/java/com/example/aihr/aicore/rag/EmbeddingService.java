package com.example.aihr.aicore.rag;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Text embedding service (P1).
 * - 接入 DashScope `text-embedding-v3`（维度 1536）
 * - 无 API Key：返回零向量并 WARN（保证 Milvus 维度一致）
 * - 失败兜底：回退确定性伪 embedding，确保联调链路仍可跑通
 */
@Service
public class EmbeddingService {
    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    public static final int DIM = 1536;

    private final RestTemplate rt;
    private final ObjectMapper om;
    private final String configuredApiKey;

    public EmbeddingService(
            @Qualifier("restTemplate") RestTemplate rt,
            ObjectMapper om,
            @Value("${spring.ai.dashscope.api-key:}") String configuredApiKey) {
        this.rt = rt;
        this.om = om;
        this.configuredApiKey = configuredApiKey;
    }

    public List<Float> embed(String text) {
        String apiKey = (configuredApiKey == null || configuredApiKey.isBlank())
                ? System.getenv("AIHR_DASHSCOPE_API_KEY")
                : configuredApiKey;
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("DashScope embedding disabled: API key not set in config/env. Return zero vector.");
            return zeroVector();
        }

        String input = text == null ? "" : text;
        try {
            String url = "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings";

            Map<String, Object> reqBody = new LinkedHashMap<>();
            reqBody.put("model", "text-embedding-v3");
            reqBody.put("input", List.of(input));
            reqBody.put("dimensions", DIM);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Object resp = rt.postForObject(url, new HttpEntity<>(reqBody, headers), Object.class);
            JsonNode root = om.valueToTree(resp);
            JsonNode arr = root.path("data");
            if (arr.isArray() && arr.size() > 0) {
                JsonNode embArr = arr.get(0).path("embedding");
                if (embArr.isArray()) {
                    List<Float> out = new ArrayList<>(DIM);
                    int i = 0;
                    for (JsonNode v : embArr) {
                        if (i >= DIM) break;
                        out.add((float) v.asDouble());
                        i++;
                    }
                    while (out.size() < DIM) out.add(0f);
                    return out;
                }
            }
            log.warn("DashScope embedding response format unexpected, fallback pseudo embed.");
            return pseudoEmbed(input);
        } catch (Exception e) {
            log.warn("DashScope embedding request failed, fallback pseudo embed. err={}", e.toString());
            return pseudoEmbed(input);
        }
    }

    public String hashHex(String text) {
        return HexFormat.of().formatHex(sha256(text == null ? "" : text));
    }

    private List<Float> zeroVector() {
        List<Float> vec = new ArrayList<>(DIM);
        for (int i = 0; i < DIM; i++) vec.add(0f);
        return vec;
    }

    private List<Float> pseudoEmbed(String text) {
        String norm = (text == null ? "" : text).trim();
        byte[] hash = sha256(norm);
        List<Float> vec = new ArrayList<>(DIM);
        for (int i = 0; i < DIM; i++) {
            int b = hash[i % hash.length] & 0xff;
            // map to [-1, 1]
            float v = (b / 127.5f) - 1.0f;
            vec.add(v);
        }
        return vec;
    }

    private static byte[] sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}




