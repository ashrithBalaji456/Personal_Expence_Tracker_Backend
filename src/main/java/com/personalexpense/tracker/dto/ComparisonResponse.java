package com.personalexpense.tracker.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComparisonResponse {
    private BigDecimal firstPeriodTotal;
    private BigDecimal secondPeriodTotal;
    private BigDecimal difference;
    private BigDecimal percentageChange;
}
