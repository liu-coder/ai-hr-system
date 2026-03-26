package com.example.aihr.salary.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 薪酬策略 policyJson 的可配置结构：支持细粒度条目定义，便于扩展为可配置计算。
 *
 * 示例（与原有扁平结构兼容）：
 * {
 *   "taxRateBps": 300,
 *   "socialInsuranceCents": 50000,
 *   "housingFundCents": 30000,
 *   "itemDefinitions": [
 *     { "itemCode": "BASE", "itemName": "基本工资", "formulaType": "INPUT", "inputKey": "baseSalaryCents" },
 *     { "itemCode": "OVERTIME", "itemName": "加班费", "formulaType": "INPUT", "inputKey": "overtimeCents" },
 *     { "itemCode": "BONUS", "itemName": "奖金", "formulaType": "INPUT", "inputKey": "bonusCents" },
 *     { "itemCode": "DEDUCTIONS", "itemName": "其他扣款", "formulaType": "INPUT", "inputKey": "deductionsCents", "negate": true },
 *     { "itemCode": "SOCIAL", "itemName": "社保", "formulaType": "POLICY", "policyKey": "socialInsuranceCents", "negate": true },
 *     { "itemCode": "HOUSING", "itemName": "公积金", "formulaType": "POLICY", "policyKey": "housingFundCents", "negate": true },
 *     { "itemCode": "TAX", "itemName": "个税", "formulaType": "RATE", "baseItem": "PRE_TAX", "rateBpsKey": "taxRateBps", "negate": true },
 *     { "itemCode": "NET", "itemName": "实发", "formulaType": "DERIVED_NET" }
 *   ]
 * }
 * 无 itemDefinitions 时仍按原有扁平字段计算（向后兼容）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SalaryPolicyDto {

    private Integer taxRateBps;
    private Long socialInsuranceCents;
    private Long housingFundCents;
    private List<ItemDef> itemDefinitions;

    public Integer getTaxRateBps() { return taxRateBps; }
    public void setTaxRateBps(Integer taxRateBps) { this.taxRateBps = taxRateBps; }
    public Long getSocialInsuranceCents() { return socialInsuranceCents; }
    public void setSocialInsuranceCents(Long socialInsuranceCents) { this.socialInsuranceCents = socialInsuranceCents; }
    public Long getHousingFundCents() { return housingFundCents; }
    public void setHousingFundCents(Long housingFundCents) { this.housingFundCents = housingFundCents; }
    public List<ItemDef> getItemDefinitions() { return itemDefinitions; }
    public void setItemDefinitions(List<ItemDef> itemDefinitions) { this.itemDefinitions = itemDefinitions; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ItemDef {
        private String itemCode;
        private String itemName;
        /** INPUT=从 input 取；POLICY=从 policy 取；RATE=按 baseItem 金额 × rateBpsKey/10000；DERIVED_NET=应发-扣款-税 */
        private String formulaType;
        private String inputKey;
        private String policyKey;
        private String baseItem;
        private String rateBpsKey;
        /** 输出为负数（扣款/税） */
        private Boolean negate;

        public String getItemCode() { return itemCode; }
        public void setItemCode(String itemCode) { this.itemCode = itemCode; }
        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }
        public String getFormulaType() { return formulaType; }
        public void setFormulaType(String formulaType) { this.formulaType = formulaType; }
        public String getInputKey() { return inputKey; }
        public void setInputKey(String inputKey) { this.inputKey = inputKey; }
        public String getPolicyKey() { return policyKey; }
        public void setPolicyKey(String policyKey) { this.policyKey = policyKey; }
        public String getBaseItem() { return baseItem; }
        public void setBaseItem(String baseItem) { this.baseItem = baseItem; }
        public String getRateBpsKey() { return rateBpsKey; }
        public void setRateBpsKey(String rateBpsKey) { this.rateBpsKey = rateBpsKey; }
        public Boolean getNegate() { return negate; }
        public void setNegate(Boolean negate) { this.negate = negate; }
    }
}
