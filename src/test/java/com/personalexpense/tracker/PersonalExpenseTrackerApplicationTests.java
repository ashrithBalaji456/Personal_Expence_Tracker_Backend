package com.personalexpense.tracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personalexpense.tracker.dto.*;

import com.personalexpense.tracker.repository.ExpenseRepository;
import com.personalexpense.tracker.repository.UserRepository;
import com.personalexpense.tracker.repository.BudgetCategoryRepository;
import com.personalexpense.tracker.repository.MonthlyIncomeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class PersonalExpenseTrackerApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private BudgetCategoryRepository budgetCategoryRepository;

    @Autowired
    private MonthlyIncomeRepository monthlyIncomeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        expenseRepository.deleteAll();
        budgetCategoryRepository.deleteAll();
        monthlyIncomeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void contextLoads() {
    }

    @Test
    public void testAuthenticationFlow() throws Exception {
        // 1. Register a user
        RegisterRequest registerReq = RegisterRequest.builder()
                .username("testuser")
                .email("testuser@example.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("testuser"));

        // 2. Try to register with same username
        RegisterRequest duplicateUserReq = RegisterRequest.builder()
                .username("testuser")
                .email("another@example.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateUserReq)))
                .andExpect(status().isConflict());

        // 3. Try to register with same email
        RegisterRequest duplicateEmailReq = RegisterRequest.builder()
                .username("anotheruser")
                .email("testuser@example.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateEmailReq)))
                .andExpect(status().isConflict());

        // 4. Login with valid credentials
        LoginRequest loginReq = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("testuser"));

        // 5. Login with invalid credentials
        LoginRequest invalidLoginReq = LoginRequest.builder()
                .username("testuser")
                .password("wrongpassword")
                .build();

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidLoginReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testExpenseCrudAndIsolation() throws Exception {
        // 1. Register User A and User B
        RegisterRequest userAReg = RegisterRequest.builder()
                .username("usera")
                .email("usera@example.com")
                .password("password123")
                .build();
        MvcResult resA = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userAReg)))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse authA = objectMapper.readValue(resA.getResponse().getContentAsString(), AuthResponse.class);
        String tokenA = "Bearer " + authA.getToken();

        RegisterRequest userBReg = RegisterRequest.builder()
                .username("userb")
                .email("userb@example.com")
                .password("password123")
                .build();
        MvcResult resB = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userBReg)))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse authB = objectMapper.readValue(resB.getResponse().getContentAsString(), AuthResponse.class);
        String tokenB = "Bearer " + authB.getToken();

        // 2. User A creates an expense
        ExpenseRequest expenseReq = ExpenseRequest.builder()
                .title("A's Lunch")
                .amount(BigDecimal.valueOf(15.50))
                .category("Groceries")
                .expenseDate(LocalDate.now())
                .notes("Tasty lunch")
                .build();

        MvcResult expRes = mockMvc.perform(post("/api/expenses")
                .header("Authorization", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(expenseReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("A's Lunch"))
                .andReturn();

        ExpenseResponse createdExpense = objectMapper.readValue(expRes.getResponse().getContentAsString(), ExpenseResponse.class);
        Long expenseId = createdExpense.getId();

        // 3. User B tries to retrieve User A's expense by ID -> should return 404 (not found / access denied)
        mockMvc.perform(get("/api/expenses/" + expenseId)
                .header("Authorization", tokenB))
                .andExpect(status().isNotFound());

        // 4. User A retrieves own expense -> should succeed
        mockMvc.perform(get("/api/expenses/" + expenseId)
                .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("A's Lunch"));

        // 5. User B tries to update User A's expense -> should return 404
        ExpenseRequest updateReq = ExpenseRequest.builder()
                .title("Hacked Lunch")
                .amount(BigDecimal.valueOf(100.00))
                .category("Travel & Commute")
                .expenseDate(LocalDate.now())
                .notes("Hacked")
                .build();

        mockMvc.perform(put("/api/expenses/" + expenseId)
                .header("Authorization", tokenB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        // 6. User A updates own expense -> should succeed
        mockMvc.perform(put("/api/expenses/" + expenseId)
                .header("Authorization", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Hacked Lunch"))
                .andExpect(jsonPath("$.category").value("Travel & Commute"))
                .andExpect(jsonPath("$.amount").value(100.00));

        // 7. User B tries to delete User A's expense -> should return 404
        mockMvc.perform(delete("/api/expenses/" + expenseId)
                .header("Authorization", tokenB))
                .andExpect(status().isNotFound());

        // 8. User A deletes own expense -> should succeed
        mockMvc.perform(delete("/api/expenses/" + expenseId)
                .header("Authorization", tokenA))
                .andExpect(status().isNoContent());

        // 9. User A tries to retrieve deleted expense -> should return 404
        mockMvc.perform(get("/api/expenses/" + expenseId)
                .header("Authorization", tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testAnalyticsAndDashboard() throws Exception {
        // Register and login
        RegisterRequest registerReq = RegisterRequest.builder()
                .username("analyticsuser")
                .email("analytics@example.com")
                .password("password123")
                .build();
        MvcResult res = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse auth = objectMapper.readValue(res.getResponse().getContentAsString(), AuthResponse.class);
        String token = "Bearer " + auth.getToken();

        LocalDate today = LocalDate.now();

        // Create expenses on different days
        ExpenseRequest todayExp = ExpenseRequest.builder()
                .title("Food Today")
                .amount(BigDecimal.valueOf(100.00))
                .category("Groceries")
                .expenseDate(today)
                .build();

        ExpenseRequest yesterdayExp = ExpenseRequest.builder()
                .title("Travel Yesterday")
                .amount(BigDecimal.valueOf(250.00))
                .category("Travel & Commute")
                .expenseDate(today.minusDays(1))
                .build();

        ExpenseRequest lastWeekExp = ExpenseRequest.builder()
                .title("Personal Last Week")
                .amount(BigDecimal.valueOf(50.00))
                .category("Other Expenses")
                .expenseDate(today.minusDays(7))
                .build();

        mockMvc.perform(post("/api/expenses").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(todayExp))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/expenses").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(yesterdayExp))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/expenses").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(lastWeekExp))).andExpect(status().isCreated());

        // Test Dashboard
        mockMvc.perform(get("/api/dashboard")
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSpent").value(400.00))
                .andExpect(jsonPath("$.todaySpent").value(100.00))
                .andExpect(jsonPath("$.yesterdaySpent").value(250.00))
                .andExpect(jsonPath("$.categoryBreakdown.Groceries").value(100.00))
                .andExpect(jsonPath("$.categoryBreakdown['Travel & Commute']").value(250.00))
                .andExpect(jsonPath("$.categoryBreakdown['Other Expenses']").value(50.00));

        // Test Analytics - custom spending range
        mockMvc.perform(get("/api/analytics/spending?startDate=" + today.minusDays(1) + "&endDate=" + today)
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSpent").value(350.00))
                .andExpect(jsonPath("$.categorySpending.Groceries").value(100.00))
                .andExpect(jsonPath("$.categorySpending['Travel & Commute']").value(250.00));

        // Test Analytics - last 3 days
        mockMvc.perform(get("/api/analytics/last-3-days")
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSpent").value(350.00))
                .andExpect(jsonPath("$.dailySpending.length()").value(3));

        // Test Analytics - compare two periods
        mockMvc.perform(get("/api/analytics/compare?firstStart=" + today.minusDays(7) + "&firstEnd=" + today.minusDays(7) +
                "&secondStart=" + today.minusDays(1) + "&secondEnd=" + today)
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstPeriodTotal").value(50.00))
                .andExpect(jsonPath("$.secondPeriodTotal").value(350.00))
                .andExpect(jsonPath("$.difference").value(300.00))
                .andExpect(jsonPath("$.percentageChange").value(600.00));
    }
}
