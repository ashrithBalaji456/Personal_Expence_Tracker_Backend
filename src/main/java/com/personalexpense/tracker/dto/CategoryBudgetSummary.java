package com.personalexpense.tracker.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryBudgetSummary {
    private Long categoryId;
    private String categoryName;
    private BigDecimal percentage;
    private BigDecimal allocatedAmount;
    private BigDecimal actualSpent;
    private BigDecimal previousCarryover;
    private BigDecimal netBalance;
    private BigDecimal cumulativeBalance;
    private String status; // "surplus" or "exceeded"
    private BigDecimal amountRemaining;
    private BigDecimal amountExceeded;
    private String color;
    private String icon;
    private List<ExpenseResponse> history;
}
