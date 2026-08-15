package com.personalexpense.tracker.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Last3DaysAnalyticsDto {
    private BigDecimal totalSpent;
    private List<DailySpendingDto> dailySpending;
}
