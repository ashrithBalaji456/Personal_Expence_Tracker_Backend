package com.personalexpense.tracker.service;

import com.personalexpense.tracker.dto.*;
import java.util.List;

public interface BudgetService {
    MonthlyIncomeResponse setMonthlyIncome(MonthlyIncomeRequest request);
    MonthlyIncomeResponse getMonthlyIncome(String month);

    List<BudgetCategoryResponse> getBudgetCategories();
    BudgetCategoryResponse addBudgetCategory(BudgetCategoryRequest request);
    BudgetCategoryResponse updateBudgetCategory(Long id, BudgetCategoryRequest request);
    void deleteBudgetCategory(Long id);
    List<BudgetCategoryResponse> resetToDefaults();

    BudgetSummaryResponse getBudgetSummary(String month);
}
