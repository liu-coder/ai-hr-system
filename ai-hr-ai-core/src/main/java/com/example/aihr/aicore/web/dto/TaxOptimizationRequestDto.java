package com.example.aihr.aicore.web.dto;

import java.util.List;

/**
 * 税务优化请求DTO
 */
public class TaxOptimizationRequestDto {
    private String employeeId;
    private double currentTaxableIncome;
    private double currentTax;
    private List<DeductionDto> deductions;
    private List<FamilyMemberDto> familyMembers;
    private String taxYear;
    
    // Getters and setters
    public String getEmployeeId() {
        return employeeId;
    }
    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }
    public double getCurrentTaxableIncome() {
        return currentTaxableIncome;
    }
    public void setCurrentTaxableIncome(double currentTaxableIncome) {
        this.currentTaxableIncome = currentTaxableIncome;
    }
    public double getCurrentTax() {
        return currentTax;
    }
    public void setCurrentTax(double currentTax) {
        this.currentTax = currentTax;
    }
    public List<DeductionDto> getDeductions() {
        return deductions;
    }
    public void setDeductions(List<DeductionDto> deductions) {
        this.deductions = deductions;
    }
    public List<FamilyMemberDto> getFamilyMembers() {
        return familyMembers;
    }
    public void setFamilyMembers(List<FamilyMemberDto> familyMembers) {
        this.familyMembers = familyMembers;
    }
    public String getTaxYear() {
        return taxYear;
    }
    public void setTaxYear(String taxYear) {
        this.taxYear = taxYear;
    }
    
    /**
     * 扣除项DTO
     */
    public static class DeductionDto {
        private String type;
        private double amount;
        private boolean claimed;
        
        // Getters and setters
        public String getType() {
            return type;
        }
        public void setType(String type) {
            this.type = type;
        }
        public double getAmount() {
            return amount;
        }
        public void setAmount(double amount) {
            this.amount = amount;
        }
        public boolean isClaimed() {
            return claimed;
        }
        public void setClaimed(boolean claimed) {
            this.claimed = claimed;
        }
    }
    
    /**
     * 家庭成员DTO
     */
    public static class FamilyMemberDto {
        private String relationship;
        private int age;
        private boolean dependent;
        
        // Getters and setters
        public String getRelationship() {
            return relationship;
        }
        public void setRelationship(String relationship) {
            this.relationship = relationship;
        }
        public int getAge() {
            return age;
        }
        public void setAge(int age) {
            this.age = age;
        }
        public boolean isDependent() {
            return dependent;
        }
        public void setDependent(boolean dependent) {
            this.dependent = dependent;
        }
    }
}



