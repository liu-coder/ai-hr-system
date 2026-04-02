package com.example.aihr.salary.service;

import org.springframework.stereotype.Service;


import java.util.Map;

@Service
public class TaxCalculator {
    
    /**
     * 计算个人所得税
     * @param taxableIncome 应纳税所得额
     * @param taxRateBps 税率（ basis points，10000 = 100%）
     * @return 个人所得税
     */
    public long calculateIncomeTax(long taxableIncome, int taxRateBps) {
        if (taxableIncome <= 0) {
            return 0;
        }
        
        // 转换税率为小数
        double taxRate = taxRateBps / 10000.0;
        
        // 计算税额
        double tax = taxableIncome * taxRate;
        
        // 四舍五入到整数
        return Math.round(tax);
    }
    
    /**
     * 计算专项扣除
     * @param employeeData 员工数据
     * @return 专项扣除金额
     */
    public long calculateSpecialDeductions(Map<String, Object> employeeData) {
        long specialDeductions = 0;
        
        // 子女教育：每个子女每月1000元
        if (employeeData.containsKey("childrenCount")) {
            int childrenCount = (int) employeeData.get("childrenCount");
            specialDeductions += childrenCount * 1000 * 12;
        }
        
        // 继续教育：每月400元
        if (employeeData.containsKey("continuingEducation") && (boolean) employeeData.get("continuingEducation")) {
            specialDeductions += 400 * 12;
        }
        
        // 住房贷款利息：每月1000元
        if (employeeData.containsKey("housingLoan") && (boolean) employeeData.get("housingLoan")) {
            specialDeductions += 1000 * 12;
        }
        
        // 住房租金：根据城市不同，每月800-1500元
        if (employeeData.containsKey("housingRent") && (boolean) employeeData.get("housingRent")) {
            String cityType = (String) employeeData.getOrDefault("cityType", "medium");
            switch (cityType) {
                case "large":
                    specialDeductions += 1500 * 12;
                    break;
                case "medium":
                    specialDeductions += 1100 * 12;
                    break;
                case "small":
                    specialDeductions += 800 * 12;
                    break;
            }
        }
        
        // 赡养老人：独生子女每月2000元，非独生子女每月1000元
        if (employeeData.containsKey("elderlySupport")) {
            boolean isOnlyChild = (boolean) employeeData.getOrDefault("isOnlyChild", false);
            specialDeductions += isOnlyChild ? 2000 * 12 : 1000 * 12;
        }
        
        return specialDeductions;
    }
    
    /**
     * 计算社会保险费
     * @param baseSalary 基本工资
     * @param socialInsuranceRateBps 社会保险费率（ basis points，10000 = 100%）
     * @return 社会保险费
     */
    public long calculateSocialInsurance(long baseSalary, int socialInsuranceRateBps) {
        if (baseSalary <= 0) {
            return 0;
        }
        
        // 转换费率为小数
        double rate = socialInsuranceRateBps / 10000.0;
        
        // 计算社会保险费
        double socialInsurance = baseSalary * rate;
        
        // 四舍五入到整数
        return Math.round(socialInsurance);
    }
    
    /**
     * 计算住房公积金
     * @param baseSalary 基本工资
     * @param housingFundRateBps 住房公积金费率（ basis points，10000 = 100%）
     * @return 住房公积金
     */
    public long calculateHousingFund(long baseSalary, int housingFundRateBps) {
        if (baseSalary <= 0) {
            return 0;
        }
        
        // 转换费率为小数
        double rate = housingFundRateBps / 10000.0;
        
        // 计算住房公积金
        double housingFund = baseSalary * rate;
        
        // 四舍五入到整数
        return Math.round(housingFund);
    }
    
    /**
     * 计算应纳税所得额
     * @param grossIncome 总收入
     * @param deductions 扣除项
     * @param socialInsurance 社会保险费
     * @param housingFund 住房公积金
     * @param specialDeductions 专项扣除
     * @return 应纳税所得额
     */
    public long calculateTaxableIncome(long grossIncome, long deductions, long socialInsurance, long housingFund, long specialDeductions) {
        // 计算应纳税所得额
        long taxableIncome = grossIncome - deductions - socialInsurance - housingFund - specialDeductions - 60000; // 60000是每年的基本减除费用
        
        // 确保应纳税所得额不为负数
        return Math.max(0, taxableIncome);
    }
    
    /**
     * 计算税后收入
     * @param grossIncome 总收入
     * @param deductions 扣除项
     * @param socialInsurance 社会保险费
     * @param housingFund 住房公积金
     * @param tax 个人所得税
     * @return 税后收入
     */
    public long calculateAfterTaxIncome(long grossIncome, long deductions, long socialInsurance, long housingFund, long tax) {
        // 计算税后收入
        return grossIncome - deductions - socialInsurance - housingFund - tax;
    }
}
