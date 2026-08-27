package com.personalexpense.tracker.service.impl;

import com.personalexpense.tracker.dto.CategorySpendingDto;
import com.personalexpense.tracker.dto.DashboardResponse;
import com.personalexpense.tracker.dto.DailySpendingDto;
import com.personalexpense.tracker.dto.ExpenseResponse;
import com.personalexpense.tracker.repository.BudgetCategoryRepository;
import com.personalexpense.tracker.entity.BudgetCategory;
import java.util.HashMap;
import com.personalexpense.tracker.entity.Expense;
import com.personalexpense.tracker.entity.User;
import com.personalexpense.tracker.exception.ResourceNotFoundException;
import com.personalexpense.tracker.repository.ExpenseRepository;
import com.personalexpense.tracker.repository.UserRepository;
import com.personalexpense.tracker.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.personalexpense.tracker.repository.MonthlyIncomeRepository;
import com.personalexpense.tracker.entity.MonthlyIncome;
import java.time.YearMonth;
import java.util.Set;
import java.util.HashSet;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final BudgetCategoryRepository budgetCategoryRepository;
    private final MonthlyIncomeRepository monthlyIncomeRepository;

    private User getAuthenticatedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboardData() {
        User user = getAuthenticatedUser();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate threeDaysAgo = today.minusDays(2);

        // Date ranges
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.with(TemporalAdjusters.lastDayOfMonth());

        LocalDate epochStart = LocalDate.of(1900, 1, 1);
        LocalDate farFutureEnd = LocalDate.of(9999, 12, 31);

        // Compute spends
        BigDecimal totalSpent = expenseRepository.sumTotalSpentByUser(user);
        BigDecimal todaySpent = expenseRepository.sumTotalSpentByUserAndDateBetween(user, today, today);
        BigDecimal yesterdaySpent = expenseRepository.sumTotalSpentByUserAndDateBetween(user, yesterday, yesterday);
        BigDecimal last3DaysSpent = expenseRepository.sumTotalSpentByUserAndDateBetween(user, threeDaysAgo, today);
        BigDecimal currentWeekSpent = expenseRepository.sumTotalSpentByUserAndDateBetween(user, weekStart, weekEnd);
        BigDecimal currentMonthSpent = expenseRepository.sumTotalSpentByUserAndDateBetween(user, monthStart, monthEnd);

        // Category breakdown
        List<CategorySpendingDto> categoryList = expenseRepository.findCategorySpendingByUserAndDateBetween(user, epochStart, farFutureEnd);
        Map<String, BigDecimal> categoryBreakdown = new HashMap<>();
        List<BudgetCategory> userCategories = budgetCategoryRepository.findByUser(user);
        for (BudgetCategory cat : userCategories) {
            categoryBreakdown.put(cat.getName(), BigDecimal.ZERO);
        }
        for (CategorySpendingDto dto : categoryList) {
            categoryBreakdown.put(dto.getCategory(), dto.getAmount());
        }

        // Daily breakdown - fetch daily spending for the last 7 days
        LocalDate sevenDaysAgo = today.minusDays(6);
        List<DailySpendingDto> dailyBreakdown = expenseRepository.findDailySpendingByUserAndDateBetween(user, sevenDaysAgo, today);

        // Recent expenses (Top 5)
        List<ExpenseResponse> recentExpenses = expenseRepository.findTop5ByUserOrderByExpenseDateDescCreatedAtDesc(user)
                .stream()
                .map(this::mapToResponse)
                .toList();

        // Compute lifetime savings
        List<Expense> allExpenses = expenseRepository.findByUserOrderByExpenseDateDescCreatedAtDesc(user);
        List<MonthlyIncome> allIncomes = monthlyIncomeRepository.findByUserOrderByMonthAsc(user);
        Set<String> activeMonths = new HashSet<>();
        for (MonthlyIncome mi : allIncomes) {
            activeMonths.add(mi.getMonth());
        }
        for (Expense e : allExpenses) {
            activeMonths.add(e.getExpenseDate().toString().substring(0, 7)); // "yyyy-MM"
        }
        activeMonths.add(YearMonth.now().toString());

        BigDecimal totalLifetimeIncome = BigDecimal.ZERO;
        for (String month : activeMonths) {
            totalLifetimeIncome = totalLifetimeIncome.add(resolveMonthlyIncomeAmount(user, month, allIncomes));
        }
        BigDecimal lifetimeSavings = totalLifetimeIncome.subtract(totalSpent);

        return DashboardResponse.builder()
                .totalSpent(totalSpent)
                .todaySpent(todaySpent)
                .yesterdaySpent(yesterdaySpent)
                .last3DaysSpent(last3DaysSpent)
                .currentWeekSpent(currentWeekSpent)
                .currentMonthSpent(currentMonthSpent)
                .lifetimeSavings(lifetimeSavings)
                .categoryBreakdown(categoryBreakdown)
                .dailyBreakdown(dailyBreakdown)
                .recentExpenses(recentExpenses)
                .build();
    }

    private BigDecimal resolveMonthlyIncomeAmount(User user, String targetMonth, List<MonthlyIncome> allIncomes) {
        Optional<MonthlyIncome> exactMatch = allIncomes.stream()
                .filter(i -> i.getMonth().equals(targetMonth))
                .findFirst();
        if (exactMatch.isPresent()) {
            return exactMatch.get().getAmount();
        }
        
        return allIncomes.stream()
                .filter(i -> i.getMonth().compareTo(targetMonth) <= 0)
                .max((a, b) -> a.getMonth().compareTo(b.getMonth()))
                .map(MonthlyIncome::getAmount)
                .orElse(BigDecimal.ZERO);
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
