package com.personalexpense.tracker.dto;

import com.personalexpense.tracker.entity.Category;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseResponse {
    private Long id;
    private String title;
    private BigDecimal amount;
    private Category category;
    private LocalDate expenseDate;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
