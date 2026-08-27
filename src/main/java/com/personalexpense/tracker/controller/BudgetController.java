package com.personalexpense.tracker.controller;

import com.personalexpense.tracker.dto.*;
import com.personalexpense.tracker.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budget")
@RequiredArgsConstructor
@Tag(name = "Budget Management", description = "Endpoints for managing monthly income, custom category percentages, and cumulative rollovers.")
@SecurityRequirement(name = "bearerAuth")
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping("/income")
    @Operation(summary = "Set or update monthly income", description = "Sets the user's total salary/income for a specific month (format YYYY-MM).")
    public ResponseEntity<MonthlyIncomeResponse> setMonthlyIncome(@Valid @RequestBody MonthlyIncomeRequest request) {
        MonthlyIncomeResponse response = budgetService.setMonthlyIncome(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/income")
    @Operation(summary = "Get monthly income", description = "Retrieves the user's recorded income for a specific month (format YYYY-MM).")
    public ResponseEntity<MonthlyIncomeResponse> getMonthlyIncome(@RequestParam String month) {
        MonthlyIncomeResponse response = budgetService.getMonthlyIncome(month);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/categories")
    @Operation(summary = "List budget categories", description = "Returns all custom budget categories and percentage allocations for the authenticated user.")
    public ResponseEntity<List<BudgetCategoryResponse>> getBudgetCategories() {
        List<BudgetCategoryResponse> response = budgetService.getBudgetCategories();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/categories")
    @Operation(summary = "Add a new budget category", description = "Creates a new custom category with a target percentage allocation.")
    public ResponseEntity<BudgetCategoryResponse> addBudgetCategory(@Valid @RequestBody BudgetCategoryRequest request) {
        BudgetCategoryResponse response = budgetService.addBudgetCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Update an existing budget category", description = "Modifies name, target percentage, color, or icon of a budget category.")
    public ResponseEntity<BudgetCategoryResponse> updateBudgetCategory(@PathVariable Long id, @Valid @RequestBody BudgetCategoryRequest request) {
        BudgetCategoryResponse response = budgetService.updateBudgetCategory(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "Delete a budget category", description = "Removes a custom budget category by ID.")
    public ResponseEntity<Void> deleteBudgetCategory(@PathVariable Long id) {
        budgetService.deleteBudgetCategory(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/categories/reset")
    @Operation(summary = "Reset categories to default", description = "Clears all custom categories and restores the 10 standard seeded defaults.")
    public ResponseEntity<List<BudgetCategoryResponse>> resetToDefaults() {
        List<BudgetCategoryResponse> response = budgetService.resetToDefaults();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get monthly budget rollover summary", description = "Calculates available envelope budgets, actual spending, carryover leftovers, and statuses for a target month.")
    public ResponseEntity<BudgetSummaryResponse> getBudgetSummary(@RequestParam String month) {
        BudgetSummaryResponse response = budgetService.getBudgetSummary(month);
        return ResponseEntity.ok(response);
    }
}
