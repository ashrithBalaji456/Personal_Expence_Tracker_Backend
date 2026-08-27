package com.personalexpense.tracker.repository;

import com.personalexpense.tracker.dto.CategorySpendingDto;
import com.personalexpense.tracker.dto.DailySpendingDto;

import com.personalexpense.tracker.entity.Expense;
import com.personalexpense.tracker.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByUserOrderByExpenseDateDescCreatedAtDesc(User user);

    Optional<Expense> findByIdAndUser(Long id, User user);

    List<Expense> findByUserAndCategoryOrderByExpenseDateDescCreatedAtDesc(User user, String category);

    List<Expense> findByUserAndExpenseDateOrderByCreatedAtDesc(User user, LocalDate date);

    List<Expense> findByUserAndExpenseDateBetweenOrderByExpenseDateDescCreatedAtDesc(User user, LocalDate startDate, LocalDate endDate);

    Page<Expense> findByUserOrderByExpenseDateDescCreatedAtDesc(User user, Pageable pageable);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user = :user")
    BigDecimal sumTotalSpentByUser(@Param("user") User user);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user = :user AND e.expenseDate BETWEEN :start AND :end")
    BigDecimal sumTotalSpentByUserAndDateBetween(@Param("user") User user, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT new com.personalexpense.tracker.dto.CategorySpendingDto(e.category, SUM(e.amount)) " +
           "FROM Expense e WHERE e.user = :user AND e.expenseDate BETWEEN :start AND :end " +
           "GROUP BY e.category")
    List<CategorySpendingDto> findCategorySpendingByUserAndDateBetween(@Param("user") User user, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT new com.personalexpense.tracker.dto.DailySpendingDto(e.expenseDate, SUM(e.amount)) " +
           "FROM Expense e WHERE e.user = :user AND e.expenseDate BETWEEN :start AND :end " +
           "GROUP BY e.expenseDate ORDER BY e.expenseDate ASC")
    List<DailySpendingDto> findDailySpendingByUserAndDateBetween(@Param("user") User user, @Param("start") LocalDate start, @Param("end") LocalDate end);

    List<Expense> findTop5ByUserOrderByExpenseDateDescCreatedAtDesc(User user);
}
