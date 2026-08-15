package com.personalexpense.tracker.service.impl;

import com.personalexpense.tracker.dto.*;
import com.personalexpense.tracker.entity.Category;
import com.personalexpense.tracker.entity.User;
import com.personalexpense.tracker.exception.ResourceNotFoundException;
import com.personalexpense.tracker.repository.ExpenseRepository;
import com.personalexpense.tracker.repository.UserRepository;
import com.personalexpense.tracker.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    private User getAuthenticatedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));
    }

    private Map<Category, BigDecimal> getCategoryMap(List<CategorySpendingDto> dtos) {
        Map<Category, BigDecimal> categoryMap = new EnumMap<>(Category.class);
        for (Category cat : Category.values()) {
            categoryMap.put(cat, BigDecimal.ZERO);
        }
        for (CategorySpendingDto dto : dtos) {
            categoryMap.put(dto.getCategory(), dto.getAmount());
        }
        return categoryMap;
    }

    @Override
    @Transactional(readOnly = true)
    public DateRangeAnalyticsDto getSpendingBetweenDates(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date.");
        }
        User user = getAuthenticatedUser();

        BigDecimal totalSpent = expenseRepository.sumTotalSpentByUserAndDateBetween(user, start, end);
        List<DailySpendingDto> dailySpending = expenseRepository.findDailySpendingByUserAndDateBetween(user, start, end);
        List<CategorySpendingDto> categoryList = expenseRepository.findCategorySpendingByUserAndDateBetween(user, start, end);

        return DateRangeAnalyticsDto.builder()
                .startDate(start)
                .endDate(end)
                .totalSpent(totalSpent)
                .dailySpending(dailySpending)
                .categorySpending(getCategoryMap(categoryList))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Last3DaysAnalyticsDto getLast3DaysSpending() {
        User user = getAuthenticatedUser();
        LocalDate today = LocalDate.now();
        LocalDate twoDaysAgo = today.minusDays(2);

        BigDecimal totalSpent = expenseRepository.sumTotalSpentByUserAndDateBetween(user, twoDaysAgo, today);
        List<DailySpendingDto> dailySpendingRaw = expenseRepository.findDailySpendingByUserAndDateBetween(user, twoDaysAgo, today);

        // Fill in missing dates with zero spending
        Map<LocalDate, BigDecimal> dailyMap = dailySpendingRaw.stream()
                .collect(Collectors.toMap(DailySpendingDto::getDate, DailySpendingDto::getAmount));

        List<DailySpendingDto> dailySpending = new ArrayList<>();
        for (int i = 2; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            dailySpending.add(new DailySpendingDto(d, dailyMap.getOrDefault(d, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))));
        }

        return Last3DaysAnalyticsDto.builder()
                .totalSpent(totalSpent)
                .dailySpending(dailySpending)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public WeeklyAnalyticsDto getWeeklySpending() {
        User user = getAuthenticatedUser();
        LocalDate today = LocalDate.now();

        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        BigDecimal totalSpent = expenseRepository.sumTotalSpentByUserAndDateBetween(user, weekStart, weekEnd);
        List<DailySpendingDto> dailySpendingRaw = expenseRepository.findDailySpendingByUserAndDateBetween(user, weekStart, weekEnd);
        List<CategorySpendingDto> categoryList = expenseRepository.findCategorySpendingByUserAndDateBetween(user, weekStart, weekEnd);

        // Fill in missing dates with zero spending for all 7 days of the week
        Map<LocalDate, BigDecimal> dailyMap = dailySpendingRaw.stream()
                .collect(Collectors.toMap(DailySpendingDto::getDate, DailySpendingDto::getAmount));

        List<DailySpendingDto> dailySpending = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = weekStart.plusDays(i);
            dailySpending.add(new DailySpendingDto(d, dailyMap.getOrDefault(d, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))));
        }

        return WeeklyAnalyticsDto.builder()
                .weekStart(weekStart)
                .weekEnd(weekEnd)
                .totalSpent(totalSpent)
                .dailySpending(dailySpending)
                .categorySpending(getCategoryMap(categoryList))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlyAnalyticsDto getMonthlySpending() {
        User user = getAuthenticatedUser();
        LocalDate today = LocalDate.now();

        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.with(TemporalAdjusters.lastDayOfMonth());
        String monthString = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        BigDecimal totalSpent = expenseRepository.sumTotalSpentByUserAndDateBetween(user, monthStart, monthEnd);
        List<DailySpendingDto> dailySpendingRaw = expenseRepository.findDailySpendingByUserAndDateBetween(user, monthStart, monthEnd);
        List<CategorySpendingDto> categoryList = expenseRepository.findCategorySpendingByUserAndDateBetween(user, monthStart, monthEnd);

        // Fill in missing dates with zero spending for all days of the month
        Map<LocalDate, BigDecimal> dailyMap = dailySpendingRaw.stream()
                .collect(Collectors.toMap(DailySpendingDto::getDate, DailySpendingDto::getAmount));

        List<DailySpendingDto> dailySpending = new ArrayList<>();
        int daysInMonth = monthEnd.getDayOfMonth();
        for (int i = 0; i < daysInMonth; i++) {
            LocalDate d = monthStart.plusDays(i);
            dailySpending.add(new DailySpendingDto(d, dailyMap.getOrDefault(d, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))));
        }

        return MonthlyAnalyticsDto.builder()
                .month(monthString)
                .totalSpent(totalSpent)
                .dailySpending(dailySpending)
                .categorySpending(getCategoryMap(categoryList))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ComparisonResponse comparePeriods(LocalDate firstStart, LocalDate firstEnd, LocalDate secondStart, LocalDate secondEnd) {
        if (firstStart.isAfter(firstEnd) || secondStart.isAfter(secondEnd)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date in both periods.");
        }
        User user = getAuthenticatedUser();

        BigDecimal firstTotal = expenseRepository.sumTotalSpentByUserAndDateBetween(user, firstStart, firstEnd);
        BigDecimal secondTotal = expenseRepository.sumTotalSpentByUserAndDateBetween(user, secondStart, secondEnd);

        BigDecimal difference = secondTotal.subtract(firstTotal);
        BigDecimal percentageChange;

        if (firstTotal.compareTo(BigDecimal.ZERO) == 0) {
            if (secondTotal.compareTo(BigDecimal.ZERO) == 0) {
                percentageChange = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            } else {
                percentageChange = BigDecimal.valueOf(100.00).setScale(2, RoundingMode.HALF_UP);
            }
        } else {
            percentageChange = difference.multiply(BigDecimal.valueOf(100))
                    .divide(firstTotal, 2, RoundingMode.HALF_UP);
        }

        return ComparisonResponse.builder()
                .firstPeriodTotal(firstTotal)
                .secondPeriodTotal(secondTotal)
                .difference(difference)
                .percentageChange(percentageChange)
                .build();
    }
}
