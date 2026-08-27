package com.personalexpense.tracker.service.impl;

import com.personalexpense.tracker.dto.ExpenseRequest;
import com.personalexpense.tracker.dto.ExpenseResponse;

import com.personalexpense.tracker.entity.Expense;
import com.personalexpense.tracker.entity.User;
import com.personalexpense.tracker.exception.ResourceNotFoundException;
import com.personalexpense.tracker.exception.UnauthorizedAccessException;
import com.personalexpense.tracker.repository.ExpenseRepository;
import com.personalexpense.tracker.repository.UserRepository;
import com.personalexpense.tracker.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    private User getAuthenticatedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));
    }

    @Override
    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request) {
        User user = getAuthenticatedUser();
        
        Expense expense = Expense.builder()
                .title(request.getTitle().trim())
                .amount(request.getAmount())
                .category(request.getCategory())
                .expenseDate(request.getExpenseDate())
                .notes(request.getNotes() != null ? request.getNotes().trim() : null)
                .user(user)
                .build();

        Expense saved = expenseRepository.save(expense);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getAllExpenses() {
        User user = getAuthenticatedUser();
        return expenseRepository.findByUserOrderByExpenseDateDescCreatedAtDesc(user).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(Long id) {
        User user = getAuthenticatedUser();
        Expense expense = expenseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new UnauthorizedAccessException("Expense record not found or access denied."));
        return mapToResponse(expense);
    }

    @Override
    @Transactional
    public ExpenseResponse updateExpense(Long id, ExpenseRequest request) {
        User user = getAuthenticatedUser();
        Expense expense = expenseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new UnauthorizedAccessException("Expense record not found or access denied."));

        expense.setTitle(request.getTitle().trim());
        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);

        Expense updated = expenseRepository.save(expense);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteExpense(Long id) {
        User user = getAuthenticatedUser();
        Expense expense = expenseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new UnauthorizedAccessException("Expense record not found or access denied."));
        expenseRepository.delete(expense);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpensesByCategory(String category) {
        User user = getAuthenticatedUser();
        return expenseRepository.findByUserAndCategoryOrderByExpenseDateDescCreatedAtDesc(user, category).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpensesByDate(LocalDate date) {
        User user = getAuthenticatedUser();
        return expenseRepository.findByUserAndExpenseDateOrderByCreatedAtDesc(user, date).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpensesInRange(LocalDate start, LocalDate end) {
        User user = getAuthenticatedUser();
        return expenseRepository.findByUserAndExpenseDateBetweenOrderByExpenseDateDescCreatedAtDesc(user, start, end).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getExpensesHistory(int page, int size) {
        User user = getAuthenticatedUser();
        Pageable pageable = PageRequest.of(page, size);
        return expenseRepository.findByUserOrderByExpenseDateDescCreatedAtDesc(user, pageable)
                .map(this::mapToResponse);
    }

    private ExpenseResponse mapToResponse(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .title(expense.getTitle())
                .amount(expense.getAmount())
                .category(expense.getCategory())
                .expenseDate(expense.getExpenseDate())
                .notes(expense.getNotes())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }
}
