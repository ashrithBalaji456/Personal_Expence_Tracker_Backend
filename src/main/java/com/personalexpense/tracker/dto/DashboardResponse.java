package com.personalexpense.tracker.dto;

import com.personalexpense.tracker.entity.Category;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {
    private BigDecimal totalSpent;
    private BigDecimal todaySpent;
    private BigDecimal yesterdaySpent;
    private BigDecimal last3DaysSpent;
    private BigDecimal currentWeekSpent;
    private BigDecimal currentMonthSpent;
    private Map<Category, BigDecimal> categoryBreakdown;
    private List<DailySpendingDto> dailyBreakdown;
    private List<ExpenseResponse> recentExpenses;
}
