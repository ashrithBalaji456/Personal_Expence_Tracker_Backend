package com.personalexpense.tracker.service;

import com.personalexpense.tracker.dto.*;

import java.time.LocalDate;

public interface AnalyticsService {

    DateRangeAnalyticsDto getSpendingBetweenDates(LocalDate start, LocalDate end);

    Last3DaysAnalyticsDto getLast3DaysSpending();

    WeeklyAnalyticsDto getWeeklySpending();

    MonthlyAnalyticsDto getMonthlySpending();

    ComparisonResponse comparePeriods(LocalDate firstStart, LocalDate firstEnd, LocalDate secondStart, LocalDate secondEnd);
}
