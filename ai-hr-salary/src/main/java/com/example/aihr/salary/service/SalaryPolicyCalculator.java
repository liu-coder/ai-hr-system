package com.example.aihr.salary.service;

import com.example.aihr.salary.model.SalaryPolicyDto;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 按策略中的 itemDefinitions 做细粒度可配置计算；无配置时返回 null，由调用方回退到骨架逻辑。
 */
@Component
public class SalaryPolicyCalculator {



    /**
     * 若 policy 含有效 itemDefinitions 则按配置计算并返回 lines；否则返回 null。
     */
    public List<SalaryService.Line> computeByItemDefinitions(
            String employeeId,
            JsonNode input,
            SalaryPolicyDto policy) {
        if (policy == null || policy.getItemDefinitions() == null || policy.getItemDefinitions().isEmpty()) {
            return null;
        }
        Map<String, Long> resolved = new LinkedHashMap<>();
        List<SalaryService.Line> lines = new ArrayList<>();

        long gross = 0;
        long deductionsTotal = 0;
        long social = policy.getSocialInsuranceCents() != null ? policy.getSocialInsuranceCents() : 0;
        long housing = policy.getHousingFundCents() != null ? policy.getHousingFundCents() : 0;

        for (SalaryPolicyDto.ItemDef def : policy.getItemDefinitions()) {
            String code = def.getItemCode();
            String name = def.getItemName() != null ? def.getItemName() : code;
            long amount;
            Map<String, Object> detail = new LinkedHashMap<>();

            switch (def.getFormulaType() != null ? def.getFormulaType().toUpperCase() : "") {
                case "INPUT" -> {
                    amount = def.getInputKey() != null ? input.path(def.getInputKey()).asLong(0) : 0;
                    if (Boolean.TRUE.equals(def.getNegate())) amount = -amount;
                    detail.put("inputKey", def.getInputKey());
                }
                case "POLICY" -> {
                    amount = getPolicyValue(policy, def.getPolicyKey());
                    if (Boolean.TRUE.equals(def.getNegate())) amount = -amount;
                    detail.put("policyKey", def.getPolicyKey());
                }
                case "RATE" -> {
                    Long base = def.getBaseItem() != null ? resolved.get(def.getBaseItem()) : null;
                    int bps = getPolicyRateBps(policy, def.getRateBpsKey());
                    amount = base != null ? Math.round(base * (bps / 10000.0)) : 0;
                    if (Boolean.TRUE.equals(def.getNegate())) amount = -amount;
                    detail.put("baseItem", def.getBaseItem());
                    detail.put("rateBpsKey", def.getRateBpsKey());
                    detail.put("taxRateBps", bps);
                }
                case "SUM" -> {
                    amount = 0;
                    if (def.getItemsToSum() != null && !def.getItemsToSum().isEmpty()) {
                        for (String itemCode : def.getItemsToSum()) {
                            Long itemAmount = resolved.get(itemCode);
                            if (itemAmount != null) {
                                amount += itemAmount;
                            }
                        }
                    }
                    if (Boolean.TRUE.equals(def.getNegate())) amount = -amount;
                    detail.put("itemsToSum", def.getItemsToSum());
                }
                case "FIXED" -> {
                    amount = def.getFixedAmount() != null ? def.getFixedAmount() : 0;
                    if (Boolean.TRUE.equals(def.getNegate())) amount = -amount;
                    detail.put("fixedAmount", def.getFixedAmount());
                }
                case "CONDITIONAL" -> {
                    amount = 0;
                    if (def.getCondition() != null) {
                        String conditionField = def.getCondition().getField();
                        String conditionOp = def.getCondition().getOp();
                        String conditionValue = def.getCondition().getValue();
                        
                        boolean conditionMet = false;
                        if ("input".equals(conditionField)) {
                            String inputKey = conditionValue.split("\\.")[1];
                            long inputValue = input.path(inputKey).asLong(0);
                            long compareValue = Long.parseLong(conditionValue.split("\\.")[2]);
                            
                            switch (conditionOp) {
                                case ">" -> conditionMet = inputValue > compareValue;
                                case "<" -> conditionMet = inputValue < compareValue;
                                case "==" -> conditionMet = inputValue == compareValue;
                                case ">=" -> conditionMet = inputValue >= compareValue;
                                case "<=" -> conditionMet = inputValue <= compareValue;
                            }
                        }
                        
                        if (conditionMet && def.getTrueAmount() != null) {
                            amount = def.getTrueAmount();
                        } else if (!conditionMet && def.getFalseAmount() != null) {
                            amount = def.getFalseAmount();
                        }
                    }
                    if (Boolean.TRUE.equals(def.getNegate())) amount = -amount;
                    detail.put("condition", def.getCondition());
                }
                case "DERIVED_PRE_TAX" -> {
                    amount = gross - deductionsTotal - social - housing;
                    if (amount < 0) amount = 0;
                    detail.put("gross", gross);
                    detail.put("deductionsTotal", deductionsTotal);
                }
                case "DERIVED_NET" -> {
                    Long tax = resolved.get("TAX");
                    long taxAbs = tax != null ? Math.abs(tax) : 0;
                    amount = gross - deductionsTotal - social - housing - taxAbs;
                    detail.put("gross", gross);
                    detail.put("deductionsTotal", deductionsTotal);
                    detail.put("taxAbs", taxAbs);
                }
                default -> {
                    amount = 0;
                }
            }

            resolved.put(code, amount);
            if ("BASE".equals(code) || "OVERTIME".equals(code) || "BONUS".equals(code) || "ALLOWANCE".equals(code) || "COMMISSION".equals(code)) {
                gross += amount;
            } else if ("DEDUCTIONS".equals(code) || "TAX".equals(code) || "SOCIAL".equals(code) || "HOUSING".equals(code)) {
                deductionsTotal += Math.abs(amount);
            }
            lines.add(new SalaryService.Line(code, name, amount, detail.isEmpty() ? null : detail));
        }
        return lines;
    }

    private static long getPolicyValue(SalaryPolicyDto policy, String key) {
        if (key == null) return 0;
        if ("socialInsuranceCents".equals(key)) return policy.getSocialInsuranceCents() != null ? policy.getSocialInsuranceCents() : 0;
        if ("housingFundCents".equals(key)) return policy.getHousingFundCents() != null ? policy.getHousingFundCents() : 0;
        return 0;
    }

    private static int getPolicyRateBps(SalaryPolicyDto policy, String key) {
        if (key == null) return 0;
        if ("taxRateBps".equals(key)) return policy.getTaxRateBps() != null ? policy.getTaxRateBps() : 0;
        return 0;
    }
}
