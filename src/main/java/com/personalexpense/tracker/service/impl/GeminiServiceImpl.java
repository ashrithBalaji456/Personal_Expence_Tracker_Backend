package com.personalexpense.tracker.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.personalexpense.tracker.dto.AiChatResponse;
import com.personalexpense.tracker.entity.BudgetCategory;
import com.personalexpense.tracker.entity.Expense;
import com.personalexpense.tracker.entity.MonthlyIncome;
import com.personalexpense.tracker.entity.User;
import com.personalexpense.tracker.exception.ResourceNotFoundException;
import com.personalexpense.tracker.repository.BudgetCategoryRepository;
import com.personalexpense.tracker.repository.ExpenseRepository;
import com.personalexpense.tracker.repository.MonthlyIncomeRepository;
import com.personalexpense.tracker.repository.UserRepository;
import com.personalexpense.tracker.service.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GeminiServiceImpl implements GeminiService {

    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final BudgetCategoryRepository budgetCategoryRepository;
    private final MonthlyIncomeRepository monthlyIncomeRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<Long, CachedAlerts> alertsCache = new ConcurrentHashMap<>();

    @Value("${gemini.api.key:}")
    private String apiKey;

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

    private String buildUserFinancialContext(User user) {
        LocalDate now = LocalDate.now();
        String currentMonthStr = YearMonth.now().toString(); // e.g. "2026-08"
        BigDecimal income = resolveMonthlyIncomeAmount(user, currentMonthStr);

        List<BudgetCategory> categories = budgetCategoryRepository.findByUser(user);
        
        LocalDate start = now.withDayOfMonth(1);
        LocalDate end = now.withDayOfMonth(now.lengthOfMonth());
        List<Expense> monthExpenses = expenseRepository.findByUserAndExpenseDateBetweenOrderByExpenseDateDescCreatedAtDesc(user, start, end);

        // Calculate spent per category
        Map<String, BigDecimal> categorySpent = monthExpenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

        StringBuilder sb = new StringBuilder();
        sb.append("--- USER FINANCIAL CONTEXT ---\n");
        sb.append(String.format("Current Month: %s\n", currentMonthStr));
        sb.append(String.format("Monthly Income: %s\n\n", income));
        
        sb.append("Category Limits and Spending:\n");
        for (BudgetCategory cat : categories) {
            BigDecimal limit = income.multiply(cat.getPercentage()).divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
            BigDecimal spent = categorySpent.getOrDefault(cat.getName(), BigDecimal.ZERO);
            sb.append(String.format("- %s: Allocation %s%% (Limit: %s), Spent: %s, Remaining: %s\n",
                    cat.getName(), cat.getPercentage(), limit, spent, limit.subtract(spent)));
        }

        sb.append("\nRecent logged outflows (expenses) this month:\n");
        int count = 0;
        for (Expense exp : monthExpenses) {
            if (count++ >= 15) break; // keep it focused
            sb.append(String.format("- %s: %s on %s (Category: %s)\n",
                    exp.getTitle(), exp.getAmount(), exp.getExpenseDate(), exp.getCategory()));
        }
        sb.append("-----------------------------\n");
        return sb.toString();
    }

    private String callGemini(String prompt) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return null;
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> parts = Map.of("parts", List.of(textPart));
        Map<String, Object> contents = Map.of("contents", List.of(parts));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(contents, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();
        } catch (Exception e) {
            return "Error calling Gemini API: " + e.getMessage();
        }
    }

    @Override
    public AiChatResponse chatWithAdvisor(String userMessage) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return AiChatResponse.builder()
                    .response("Gemini AI features are currently offline. Please configure your `GEMINI_API_KEY` in the backend `application.properties` or environment variables to unlock the AI Financial Advisor chat.")
                    .active(false)
                    .build();
        }

        User user = getAuthenticatedUser();
        String context = buildUserFinancialContext(user);

        String systemPrompt = "You are a friendly, professional Personal Financial Advisor. " +
                "Use the following user financial details to answer their query.\n\n" +
                context + "\n\n" +
                "Guidelines for your response:\n" +
                "1. Use simple, plain English that is easy to understand. Do NOT use difficult financial terms or complex jargon (avoid words like velocity, projections, amortization, leverage, etc.).\n" +
                "2. Double check the values in the context: 'Spent' is what they have actually spent, and 'Remaining' is what is left. If Spent is 0, they have not spent any money yet. Do not confuse Spent with Remaining or Limit.\n" +
                "3. Provide clear, encouraging, and highly structured advice.\n\n" +
                "User Query: " + userMessage;

        String answer = callGemini(systemPrompt);
        return AiChatResponse.builder()
                .response(answer != null ? answer : "Error getting response from Gemini.")
                .active(true)
                .build();
    }

    @Override
    public List<String> getPredictiveAlerts() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return List.of("Gemini API Key is not configured. Setup instructions can be found in the AI Advisor page.");
        }

        User user = getAuthenticatedUser();
        
        // Cache duration: 10 minutes (600,000 ms)
        long cacheDurationMs = 600000;
        CachedAlerts cached = alertsCache.get(user.getId());
        if (cached != null && !cached.isExpired(cacheDurationMs)) {
            return cached.alerts;
        }

        String context = buildUserFinancialContext(user);

        String prompt = "You are a friendly Budgeting Assistant. Review the user's monthly limits and actual spending:\n\n" +
                context + "\n\n" +
                "Generate exactly 2-3 short, helpful bullet alerts about their spending rules:\n" +
                "1. Use simple, everyday English. Do NOT use terms like 'velocity', 'leverage', or complex jargon.\n" +
                "2. Carefully verify the 'Spent' value for each category. If Spent is 0, they have not spent anything yet. Do NOT warn about overspending in categories with 0 spent. Instead, suggest keeping it that way or congratulate them.\n" +
                "3. Keep each alert short and simple (under 15 words).\n" +
                "4. Do NOT write any introduction or greetings. Just output the bullets.";

        String response = callGemini(prompt);
        if (response == null || response.startsWith("Error calling Gemini API:")) {
            // Serve grace fallback from expired cache if available
            if (cached != null) {
                return cached.alerts;
            }
            return List.of("Gemini rate limit exceeded. Please wait a moment for live forecasts.");
        }

        // Clean up response into clean bullets
        List<String> alerts = Arrays.stream(response.split("\n"))
                .map(String::trim)
                .filter(line -> line.startsWith("-") || line.startsWith("*") || !line.isEmpty())
                .map(line -> line.replaceAll("^[-*]\\s*", "")) // remove bullet prefix
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());

        if (!alerts.isEmpty()) {
            alertsCache.put(user.getId(), new CachedAlerts(alerts));
        }

        return alerts;
    }

    private static class CachedAlerts {
        final List<String> alerts;
        final long timestamp;

        CachedAlerts(List<String> alerts) {
            this.alerts = alerts;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired(long durationMs) {
            return System.currentTimeMillis() - timestamp > durationMs;
        }
    }
}
