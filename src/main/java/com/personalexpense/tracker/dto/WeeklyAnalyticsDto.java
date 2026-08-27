package com.personalexpense.tracker.dto;


import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyAnalyticsDto {
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private BigDecimal totalSpent;
    private List<DailySpendingDto> dailySpending;
    private Map<String, BigDecimal> categorySpending;
}
