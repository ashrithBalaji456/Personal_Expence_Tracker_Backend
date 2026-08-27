package com.personalexpense.tracker.repository;

import com.personalexpense.tracker.entity.BudgetCategory;
import com.personalexpense.tracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetCategoryRepository extends JpaRepository<BudgetCategory, Long> {
    List<BudgetCategory> findByUser(User user);
    Optional<BudgetCategory> findByIdAndUser(Long id, User user);
    Optional<BudgetCategory> findByUserAndNameIgnoreCase(User user, String name);
    void deleteByUser(User user);
}
