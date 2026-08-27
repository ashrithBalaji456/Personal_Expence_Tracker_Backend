package com.personalexpense.tracker.controller;

import com.personalexpense.tracker.dto.AiChatRequest;
import com.personalexpense.tracker.dto.AiChatResponse;
import com.personalexpense.tracker.service.GeminiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Gemini AI", description = "Endpoints for interacting with Gemini AI Financial Advisor and Predictive Alerts")
public class AiController {

    private final GeminiService geminiService;

    @PostMapping("/chat")
    @Operation(summary = "Chat with AI Advisor", description = "Asks the Gemini AI Advisor a financial question with your active transaction ledger context.")
    public ResponseEntity<AiChatResponse> chatWithAdvisor(@RequestBody AiChatRequest request) {
        AiChatResponse response = geminiService.chatWithAdvisor(request.getMessage());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/alerts")
    @Operation(summary = "Get AI predictive alerts", description = "Returns dynamic budget forecasting alerts based on current month spending velocity.")
    public ResponseEntity<List<String>> getPredictiveAlerts() {
        List<String> response = geminiService.getPredictiveAlerts();
        return ResponseEntity.ok(response);
    }
}
