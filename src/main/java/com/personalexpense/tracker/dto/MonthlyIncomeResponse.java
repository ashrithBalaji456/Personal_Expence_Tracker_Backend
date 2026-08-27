package com.personalexpense.tracker.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyIncomeResponse {
    private Long id;
    private BigDecimal amount;
    private String month;
}
