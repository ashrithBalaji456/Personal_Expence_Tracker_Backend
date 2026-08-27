package com.personalexpense.tracker.service;

import com.personalexpense.tracker.dto.ExpenseRequest;
import com.personalexpense.tracker.dto.ExpenseResponse;

import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseService {

    ExpenseResponse createExpense(ExpenseRequest request);

    List<ExpenseResponse> getAllExpenses();

    ExpenseResponse getExpenseById(Long id);

    ExpenseResponse updateExpense(Long id, ExpenseRequest request);

    void deleteExpense(Long id);

    List<ExpenseResponse> getExpensesByCategory(String category);

    List<ExpenseResponse> getExpensesByDate(LocalDate date);

    List<ExpenseResponse> getExpensesInRange(LocalDate start, LocalDate end);

    Page<ExpenseResponse> getExpensesHistory(int page, int size);
}
