package com.example.aihr.aicore.web.dto;

import java.util.List;

/**
 * 路由决策：便于前端展示“为何走该路由”，为后续意图分类器预留扩展。
 */
public record RouteDecisionDto(
        /** 路由：ATTENDANCE / SALARY / POLICY_RAG */
        String route,
        /** 可读意图标签，供前端展示 */
        String intentLabel,
        /** 命中的关键词，用于可解释 */
        List<String> matchedKeywords
) {}



