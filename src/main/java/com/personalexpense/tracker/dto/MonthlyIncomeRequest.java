package com.personalexpense.tracker.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyIncomeRequest {
    @NotNull(message = "Amount is required")
    @PositiveOrZero(message = "Amount must be greater than or equal to 0")
    private BigDecimal amount;

    @NotBlank(message = "Month is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "Month must be in YYYY-MM format")
    private String month;
}
