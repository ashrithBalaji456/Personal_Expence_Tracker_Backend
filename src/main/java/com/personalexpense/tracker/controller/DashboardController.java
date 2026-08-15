package com.personalexpense.tracker.controller;

import com.personalexpense.tracker.dto.DashboardResponse;
import com.personalexpense.tracker.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Dashboard", description = "Endpoints for retrieving main dashboard aggregate metrics")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Get dashboard analytics summary", description = "Computes and returns user-specific summaries, including totals (today, week, month), category splits, daily timeline chart data, and top transactions.")
    public ResponseEntity<DashboardResponse> getDashboardData() {
        DashboardResponse response = dashboardService.getDashboardData();
        return ResponseEntity.ok(response);
    }
}
