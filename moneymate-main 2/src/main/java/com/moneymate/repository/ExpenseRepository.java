package com.moneymate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.moneymate.entity.Expense;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByUser_Id(Long userId);

    List<Expense> findByUser_IdAndSpendDateBetween(Long userId, LocalDate start, LocalDate end);

    List<Expense> findByUser_IdAndCategory(Long userId, String category);
}
