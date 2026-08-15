package com.personalexpense.tracker.controller;

import com.personalexpense.tracker.dto.*;
import com.personalexpense.tracker.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Analytics", description = "Endpoints for analyzing and comparing user spending habits over different time periods")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/spending")
    @Operation(summary = "Get spending summary between two dates", description = "Returns total spending, daily breakdown, and category breakdown between startDate and endDate.")
    public ResponseEntity<DateRangeAnalyticsDto> getSpendingBetweenDates(
            @RequestParam String startDate,
            @RequestParam String endDate
    ) {
        DateRangeAnalyticsDto response = analyticsService.getSpendingBetweenDates(
                LocalDate.parse(startDate),
                LocalDate.parse(endDate)
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/last-3-days")
    @Operation(summary = "Get spending summary for the last 3 days", description = "Returns total spending and daily breakdown for today and the previous 2 days (filled with zero if empty).")
    public ResponseEntity<Last3DaysAnalyticsDto> getLast3DaysSpending() {
        Last3DaysAnalyticsDto response = analyticsService.getLast3DaysSpending();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/weekly")
    @Operation(summary = "Get spending summary for the current week", description = "Returns total spending, daily breakdown, and category breakdown for the current Monday-Sunday week.")
    public ResponseEntity<WeeklyAnalyticsDto> getWeeklySpending() {
        WeeklyAnalyticsDto response = analyticsService.getWeeklySpending();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/monthly")
    @Operation(summary = "Get spending summary for the current month", description = "Returns total spending, daily breakdown, and category breakdown for the current calendar month.")
    public ResponseEntity<MonthlyAnalyticsDto> getMonthlySpending() {
        MonthlyAnalyticsDto response = analyticsService.getMonthlySpending();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/compare")
    @Operation(summary = "Compare spending between two custom periods", description = "Returns spending totals for two separate periods, the difference, and the percentage change.")
    public ResponseEntity<ComparisonResponse> comparePeriods(
            @RequestParam String firstStart,
            @RequestParam String firstEnd,
            @RequestParam String secondStart,
            @RequestParam String secondEnd
    ) {
        ComparisonResponse response = analyticsService.comparePeriods(
                LocalDate.parse(firstStart),
                LocalDate.parse(firstEnd),
                LocalDate.parse(secondStart),
                LocalDate.parse(secondEnd)
        );
        return ResponseEntity.ok(response);
    }
}
