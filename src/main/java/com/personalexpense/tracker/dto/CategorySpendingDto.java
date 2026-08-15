package com.personalexpense.tracker.dto;

import com.personalexpense.tracker.entity.Category;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorySpendingDto {
    private Category category;
    private BigDecimal amount;
}
