package com.example.aihr.salary.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.example.aihr.salary.model.SalaryPolicyDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class SalaryPolicyCalculatorTest {
    private final SalaryPolicyCalculator calculator = new SalaryPolicyCalculator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void returnsNullWhenPolicyHasNoItemDefinitions() throws Exception {
        SalaryPolicyDto policy = objectMapper.readValue("""
                {
                  "taxRateBps": 300
                }
                """, SalaryPolicyDto.class);

        assertNull(calculator.computeByItemDefinitions("emp-1", objectMapper.readTree("{}"), policy));
    }

    @Test
    void computesConfiguredLinesAndDerivedNet() throws Exception {
        SalaryPolicyDto policy = objectMapper.readValue("""
                {
                  "taxRateBps": 300,
                  "socialInsuranceCents": 50000,
                  "housingFundCents": 30000,
                  "itemDefinitions": [
                    { "itemCode": "BASE", "itemName": "基本工资", "formulaType": "INPUT", "inputKey": "baseSalaryCents" },
                    { "itemCode": "BONUS", "itemName": "奖金", "formulaType": "FIXED", "fixedAmount": 20000 },
                    { "itemCode": "SOCIAL", "itemName": "社保", "formulaType": "POLICY", "policyKey": "socialInsuranceCents", "negate": true },
                    { "itemCode": "ALLOWANCE", "itemName": "津贴", "formulaType": "INPUT", "inputKey": "allowanceCents" },
                    { "itemCode": "GROSS", "itemName": "应发", "formulaType": "SUM", "itemsToSum": ["BASE", "BONUS", "ALLOWANCE"] },
                    { "itemCode": "TAX", "itemName": "个税", "formulaType": "RATE", "baseItem": "BASE", "rateBpsKey": "taxRateBps", "negate": true }
                  ]
                }
                """, SalaryPolicyDto.class);

        List<SalaryService.Line> lines = calculator.computeByItemDefinitions(
                "emp-1",
                objectMapper.readTree("""
                        {
                          "baseSalaryCents": 100000,
                          "allowanceCents": 5000
                        }
                        """),
                policy);

        assertEquals(6, lines.size());
        assertEquals(100000L, amountOf(lines, "BASE"));
        assertEquals(20000L, amountOf(lines, "BONUS"));
        assertEquals(-50000L, amountOf(lines, "SOCIAL"));
        assertEquals(5000L, amountOf(lines, "ALLOWANCE"));
        assertEquals(125000L, amountOf(lines, "GROSS"));
        assertEquals(-3000L, amountOf(lines, "TAX"));
    }

    private static long amountOf(List<SalaryService.Line> lines, String itemCode) {
        return lines.stream()
                .filter(line -> itemCode.equals(line.itemCode()))
                .findFirst()
                .orElseThrow()
                .amountCents();
    }
}
