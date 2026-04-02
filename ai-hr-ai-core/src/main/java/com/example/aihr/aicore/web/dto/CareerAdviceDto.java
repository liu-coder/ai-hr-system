package com.example.aihr.aicore.web.dto;

import java.util.List;

/**
 * 职业建议DTO
 */
public class CareerAdviceDto {
    private String adviceType;
    private String description;
    private List<String> specificActions;
    
    // Getters and setters
    public String getAdviceType() {
        return adviceType;
    }
    public void setAdviceType(String adviceType) {
        this.adviceType = adviceType;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public List<String> getSpecificActions() {
        return specificActions;
    }
    public void setSpecificActions(List<String> specificActions) {
        this.specificActions = specificActions;
    }
}



