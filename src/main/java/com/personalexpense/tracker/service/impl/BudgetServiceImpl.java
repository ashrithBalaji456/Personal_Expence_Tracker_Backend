package com.personalexpense.tracker.service.impl;

import com.personalexpense.tracker.dto.*;
import com.personalexpense.tracker.entity.BudgetCategory;
import com.personalexpense.tracker.entity.MonthlyIncome;
import com.personalexpense.tracker.entity.Expense;
import com.personalexpense.tracker.entity.User;
import com.personalexpense.tracker.exception.DuplicateResourceException;
import com.personalexpense.tracker.exception.ResourceNotFoundException;
import com.personalexpense.tracker.repository.BudgetCategoryRepository;
import com.personalexpense.tracker.repository.MonthlyIncomeRepository;
import com.personalexpense.tracker.repository.ExpenseRepository;
import com.personalexpense.tracker.repository.UserRepository;
import com.personalexpense.tracker.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final UserRepository userRepository;
    private final MonthlyIncomeRepository monthlyIncomeRepository;
    private final BudgetCategoryRepository budgetCategoryRepository;
    private final ExpenseRepository expenseRepository;

    private User getAuthenticatedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));
    }

    private BigDecimal resolveMonthlyIncomeAmount(User user, String targetMonth) {
        Optional<MonthlyIncome> exactMatch = monthlyIncomeRepository.findByUserAndMonth(user, targetMonth);
        if (exactMatch.isPresent()) {
            return exactMatch.get().getAmount();
        }
        
        List<MonthlyIncome> allIncomes = monthlyIncomeRepository.findByUserOrderByMonthAsc(user);
        return allIncomes.stream()
                .filter(i -> i.getMonth().compareTo(targetMonth) <= 0)
                .max((a, b) -> a.getMonth().compareTo(b.getMonth()))
                .map(MonthlyIncome::getAmount)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional
    public MonthlyIncomeResponse setMonthlyIncome(MonthlyIncomeRequest request) {
        User user = getAuthenticatedUser();
        MonthlyIncome income = monthlyIncomeRepository.findByUserAndMonth(user, request.getMonth())
                .orElse(MonthlyIncome.builder().user(user).month(request.getMonth()).build());
        income.setAmount(request.getAmount());
        monthlyIncomeRepository.save(income);
        return mapToIncomeResponse(income);
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlyIncomeResponse getMonthlyIncome(String month) {
        User user = getAuthenticatedUser();
        BigDecimal resolvedAmt = resolveMonthlyIncomeAmount(user, month);
        MonthlyIncomeResponse response = MonthlyIncomeResponse.builder()
                .id(null)
                .month(month)
                .amount(resolvedAmt)
                .build();
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetCategoryResponse> getBudgetCategories() {
        User user = getAuthenticatedUser();
        return budgetCategoryRepository.findByUser(user).stream()
                .map(this::mapToCategoryResponse)
                .toList();
    }

    @Override
    @Transactional
    public BudgetCategoryResponse addBudgetCategory(BudgetCategoryRequest request) {
        User user = getAuthenticatedUser();
        String name = request.getName().trim();

        if (budgetCategoryRepository.findByUserAndNameIgnoreCase(user, name).isPresent()) {
            throw new DuplicateResourceException("Category '" + name + "' already exists.");
        }

        BudgetCategory category = BudgetCategory.builder()
                .user(user)
                .name(name)
                .percentage(request.getPercentage())
                .color(request.getColor() != null ? request.getColor() : "#64748B")
                .icon(request.getIcon() != null ? request.getIcon() : "Wallet")
                .build();

        budgetCategoryRepository.save(category);
        return mapToCategoryResponse(category);
    }

    @Override
    @Transactional
    public BudgetCategoryResponse updateBudgetCategory(Long id, BudgetCategoryRequest request) {
        User user = getAuthenticatedUser();
        BudgetCategory category = budgetCategoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found or access denied."));

        String newName = request.getName().trim();
        if (!category.getName().equalsIgnoreCase(newName)) {
            if (budgetCategoryRepository.findByUserAndNameIgnoreCase(user, newName).isPresent()) {
                throw new DuplicateResourceException("Category '" + newName + "' already exists.");
            }
        }

        category.setName(newName);
        category.setPercentage(request.getPercentage());
        if (request.getColor() != null) category.setColor(request.getColor());
        if (request.getIcon() != null) category.setIcon(request.getIcon());

        budgetCategoryRepository.save(category);
        return mapToCategoryResponse(category);
    }

    @Override
    @Transactional
    public void deleteBudgetCategory(Long id) {
        User user = getAuthenticatedUser();
        BudgetCategory category = budgetCategoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found or access denied."));
        budgetCategoryRepository.delete(category);
    }

    @Override
    @Transactional
    public List<BudgetCategoryResponse> resetToDefaults() {
        User user = getAuthenticatedUser();
        budgetCategoryRepository.deleteByUser(user);

        List<BudgetCategory> defaults = Arrays.asList(
            BudgetCategory.builder().user(user).name("Rent").percentage(BigDecimal.valueOf(22)).color("#EC4899").icon("Home").build(),
            BudgetCategory.builder().user(user).name("Groceries").percentage(BigDecimal.valueOf(13)).color("#10B981").icon("ShoppingCart").build(),
            BudgetCategory.builder().user(user).name("Electricity + Wi-Fi").percentage(BigDecimal.valueOf(3)).color("#F59E0B").icon("Lightbulb").build(),
            BudgetCategory.builder().user(user).name("Term Insurance").percentage(BigDecimal.valueOf(2)).color("#EF4444").icon("Shield").build(),
            BudgetCategory.builder().user(user).name("SIP Investment").percentage(BigDecimal.valueOf(18)).color("#8B5CF6").icon("TrendingUp").build(),
            BudgetCategory.builder().user(user).name("Gold Saving").percentage(BigDecimal.valueOf(4)).color("#EAB308").icon("Coins").build(),
            BudgetCategory.builder().user(user).name("Parents Support").percentage(BigDecimal.valueOf(17)).color("#6366F1").icon("Heart").build(),
            BudgetCategory.builder().user(user).name("FD/Emergency").percentage(BigDecimal.valueOf(4)).color("#14B8A6").icon("PiggyBank").build(),
            BudgetCategory.builder().user(user).name("Travel & Commute").percentage(BigDecimal.valueOf(7)).color("#3B82F6").icon("Train").build(),
            BudgetCategory.builder().user(user).name("Other Expenses").percentage(BigDecimal.valueOf(10)).color("#64748B").icon("Wallet").build()
        );
        budgetCategoryRepository.saveAll(defaults);

        return defaults.stream().map(this::mapToCategoryResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetSummaryResponse getBudgetSummary(String targetMonth) {
        User user = getAuthenticatedUser();

        // 1. Gather all activity months to run historical cumulative carryovers
        TreeSet<String> allMonths = new TreeSet<>();
        
        // Add all months with income
        List<MonthlyIncome> incomes = monthlyIncomeRepository.findByUserOrderByMonthAsc(user);
        for (MonthlyIncome inc : incomes) {
            allMonths.add(inc.getMonth());
        }

        // Add all months with expenses
        List<Expense> allExpenses = expenseRepository.findByUserOrderByExpenseDateDescCreatedAtDesc(user);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        for (Expense exp : allExpenses) {
            allMonths.add(exp.getExpenseDate().format(formatter));
        }

        // Always ensure target month is included
        allMonths.add(targetMonth);

        // 2. Query budget categories currently active for the user
        List<BudgetCategory> categories = budgetCategoryRepository.findByUser(user);

        // 3. Chronological processing of carryovers
        Map<String, BigDecimal> cumulativeBalances = new HashMap<>();
        for (BudgetCategory cat : categories) {
            cumulativeBalances.put(cat.getName(), BigDecimal.ZERO);
        }

        Map<String, BigDecimal> previousCarryovers = new HashMap<>();
        for (BudgetCategory cat : categories) {
            previousCarryovers.put(cat.getName(), BigDecimal.ZERO);
        }

        BigDecimal targetIncome = BigDecimal.ZERO;
        List<Expense> targetExpenses = new ArrayList<>();

        for (String m : allMonths) {
            if (m.compareTo(targetMonth) > 0) {
                break; // stop after targetMonth
            }

            // Record carryovers from immediately preceding month
            if (m.equals(targetMonth)) {
                for (BudgetCategory cat : categories) {
                    previousCarryovers.put(cat.getName(), cumulativeBalances.getOrDefault(cat.getName(), BigDecimal.ZERO));
                }
            }

            // Get income for month m
            BigDecimal incomeAmt = resolveMonthlyIncomeAmount(user, m);

            if (m.equals(targetMonth)) {
                targetIncome = incomeAmt;
            }

            // Fetch expenses in month m
            YearMonth ym = YearMonth.parse(m);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            List<Expense> monthExpenses = expenseRepository.findByUserAndExpenseDateBetweenOrderByExpenseDateDescCreatedAtDesc(user, start, end);

            if (m.equals(targetMonth)) {
                targetExpenses = monthExpenses;
            }

            // Map expenditures by category
            Map<String, BigDecimal> spendsByCategory = new HashMap<>();
            for (Expense exp : monthExpenses) {
                String catName = exp.getCategory();
                spendsByCategory.put(catName, spendsByCategory.getOrDefault(catName, BigDecimal.ZERO).add(exp.getAmount()));
            }

            // Update cumulative balance for each category
            for (BudgetCategory cat : categories) {
                BigDecimal allocated = incomeAmt.multiply(cat.getPercentage()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal spent = spendsByCategory.getOrDefault(cat.getName(), BigDecimal.ZERO);
                BigDecimal net = allocated.subtract(spent);

                BigDecimal currentCum = cumulativeBalances.getOrDefault(cat.getName(), BigDecimal.ZERO);
                cumulativeBalances.put(cat.getName(), currentCum.add(net));
            }
        }

        // 4. Construct response DTO for target month
        List<CategoryBudgetSummary> catSummaries = new ArrayList<>();
        BigDecimal totalBudgeted = BigDecimal.ZERO;
        BigDecimal totalSpent = BigDecimal.ZERO;

        // Group target expenses by category for history listings
        Map<String, List<ExpenseResponse>> expensesHistoryMap = new HashMap<>();
        for (Expense exp : targetExpenses) {
            expensesHistoryMap.computeIfAbsent(exp.getCategory(), k -> new ArrayList<>()).add(mapToExpenseResponse(exp));
        }

        for (BudgetCategory cat : categories) {
            BigDecimal allocated = targetIncome.multiply(cat.getPercentage()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal spent = targetExpenses.stream()
                    .filter(e -> e.getCategory().equalsIgnoreCase(cat.getName()))
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal carryover = previousCarryovers.getOrDefault(cat.getName(), BigDecimal.ZERO);
            BigDecimal net = allocated.subtract(spent);
            BigDecimal cumulative = carryover.add(net);

            totalBudgeted = totalBudgeted.add(allocated);
            totalSpent = totalSpent.add(spent);

            String status = cumulative.compareTo(BigDecimal.ZERO) >= 0 ? "surplus" : "exceeded";
            BigDecimal amountRemaining = status.equals("surplus") ? cumulative : BigDecimal.ZERO;
            BigDecimal amountExceeded = status.equals("exceeded") ? cumulative.negate() : BigDecimal.ZERO;

            catSummaries.add(CategoryBudgetSummary.builder()
                    .categoryId(cat.getId())
                    .categoryName(cat.getName())
                    .percentage(cat.getPercentage())
                    .allocatedAmount(allocated)
                    .actualSpent(spent)
                    .previousCarryover(carryover)
                    .netBalance(net)
                    .cumulativeBalance(cumulative)
                    .status(status)
                    .amountRemaining(amountRemaining)
                    .amountExceeded(amountExceeded)
                    .color(cat.getColor())
                    .icon(cat.getIcon())
                    .history(expensesHistoryMap.getOrDefault(cat.getName(), new ArrayList<>()))
                    .build());
        }

        BigDecimal overallRemaining = targetIncome.subtract(totalSpent);

        return BudgetSummaryResponse.builder()
                .month(targetMonth)
                .totalIncome(targetIncome)
                .totalBudgeted(totalBudgeted)
                .totalSpent(totalSpent)
                .overallRemaining(overallRemaining)
                .categories(catSummaries)
                .build();
    }

    private MonthlyIncomeResponse mapToIncomeResponse(MonthlyIncome income) {
        return MonthlyIncomeResponse.builder()
                .id(income.getId())
                .amount(income.getAmount())
                .month(income.getMonth())
                .build();
    }

    private BudgetCategoryResponse mapToCategoryResponse(BudgetCategory category) {
        return BudgetCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .percentage(category.getPercentage())
                .color(category.getColor())
                .icon(category.getIcon())
                .build();
    }

    private ExpenseResponse mapToExpenseResponse(Expense expense) {
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
