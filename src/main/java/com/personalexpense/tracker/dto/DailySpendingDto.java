package com.personalexpense.tracker.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailySpendingDto {
    private LocalDate date;
    private BigDecimal amount;
}
