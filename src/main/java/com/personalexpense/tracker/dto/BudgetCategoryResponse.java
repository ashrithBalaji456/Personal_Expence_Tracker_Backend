package com.personalexpense.tracker.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetCategoryResponse {
    private Long id;
    private String name;
    private BigDecimal percentage;
    private String color;
    private String icon;
}
