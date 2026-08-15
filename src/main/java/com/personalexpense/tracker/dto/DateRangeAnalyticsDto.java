package com.personalexpense.tracker.dto;

import com.personalexpense.tracker.entity.Category;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DateRangeAnalyticsDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalSpent;
    private List<DailySpendingDto> dailySpending;
    private Map<Category, BigDecimal> categorySpending;
}
