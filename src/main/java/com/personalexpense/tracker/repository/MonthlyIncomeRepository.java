package com.personalexpense.tracker.repository;

import com.personalexpense.tracker.entity.MonthlyIncome;
import com.personalexpense.tracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlyIncomeRepository extends JpaRepository<MonthlyIncome, Long> {
    Optional<MonthlyIncome> findByUserAndMonth(User user, String month);
    List<MonthlyIncome> findByUserOrderByMonthAsc(User user);
}
