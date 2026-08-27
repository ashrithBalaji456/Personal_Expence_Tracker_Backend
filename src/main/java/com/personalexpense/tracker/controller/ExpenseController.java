package com.personalexpense.tracker.controller;

import com.personalexpense.tracker.dto.ExpenseRequest;
import com.personalexpense.tracker.dto.ExpenseResponse;

import com.personalexpense.tracker.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Expenses", description = "Endpoints for managing personal daily expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    @Operation(summary = "Create a new daily expense", description = "Associates the created expense record with the authenticated user.")
    public ResponseEntity<ExpenseResponse> createExpense(@Valid @RequestBody ExpenseRequest request) {
        ExpenseResponse response = expenseService.createExpense(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Retrieve all expenses", description = "Returns all expenses belonging to the authenticated user, sorted by date and creation time.")
    public ResponseEntity<List<ExpenseResponse>> getAllExpenses() {
        List<ExpenseResponse> response = expenseService.getAllExpenses();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve a specific expense by ID", description = "Verifies ownership. Returns 404 if not found or access denied.")
    public ResponseEntity<ExpenseResponse> getExpenseById(@PathVariable Long id) {
        ExpenseResponse response = expenseService.getExpenseById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing expense by ID", description = "Updates details. Verifies ownership.")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseRequest request
    ) {
        ExpenseResponse response = expenseService.updateExpense(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an expense by ID", description = "Deletes expense record. Verifies ownership.")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Filter expenses by category", description = "Returns authenticated user's expenses matching the category enum.")
    public ResponseEntity<List<ExpenseResponse>> getExpensesByCategory(@PathVariable String category) {
        List<ExpenseResponse> response = expenseService.getExpensesByCategory(category);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/date/{date}")
    @Operation(summary = "Filter expenses by a specific date", description = "Returns authenticated user's expenses on the specified LocalDate.")
    public ResponseEntity<List<ExpenseResponse>> getExpensesByDate(@PathVariable String date) {
        List<ExpenseResponse> response = expenseService.getExpensesByDate(LocalDate.parse(date));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/range")
    @Operation(summary = "Filter expenses between two dates (inclusive)", description = "Returns authenticated user's expenses between startDate and endDate.")
    public ResponseEntity<List<ExpenseResponse>> getExpensesInRange(
            @RequestParam String startDate,
            @RequestParam String endDate
    ) {
        List<ExpenseResponse> response = expenseService.getExpensesInRange(
                LocalDate.parse(startDate),
                LocalDate.parse(endDate)
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    @Operation(summary = "Retrieve complete expense history (paginated)", description = "Returns paginated list of user's expenses sorted newest first.")
    public ResponseEntity<Page<ExpenseResponse>> getExpensesHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<ExpenseResponse> response = expenseService.getExpensesHistory(page, size);
        return ResponseEntity.ok(response);
    }
}
