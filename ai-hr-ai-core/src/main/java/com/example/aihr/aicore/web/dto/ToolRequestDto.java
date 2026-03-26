package com.example.aihr.aicore.web.dto;

import java.util.Map;

public class ToolRequestDto {
    private String route;
    private Map<String, Object> params;

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
}
