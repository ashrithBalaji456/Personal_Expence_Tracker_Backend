package com.personalexpense.tracker.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetSummaryResponse {
    private String month;
    private BigDecimal totalIncome;
    private BigDecimal totalBudgeted;
    private BigDecimal totalSpent;
    private BigDecimal overallRemaining;
    private List<CategoryBudgetSummary> categories;
}
