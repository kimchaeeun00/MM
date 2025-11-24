package com.moneymate.controller;

import com.moneymate.entity.Expense;
import com.moneymate.service.ExpenseService;
import com.moneymate.service.HuggingFaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/strategy")
public class StrategyController {

    @Autowired
    private HuggingFaceService huggingFaceService;

    @Autowired
    private ExpenseService expenseService;

    @GetMapping("/{userId}/{year}/{month}")
    public ResponseEntity<Map<String, String>> getStrategy(
            @PathVariable Long userId,
            @PathVariable int year,
            @PathVariable int month,
            @RequestParam(required = false, defaultValue = "0") Integer budget) {

        try {
            List<Expense> expenses = expenseService.getMonthExpenses(userId, year, month);

            int totalSpent = expenses.stream()
                .mapToInt(Expense::getAmount)
                .sum();

            Map<String, Integer> categorySpending = new HashMap<>();
            expenses.forEach(expense -> {
                String category = expense.getCategory() != null ? expense.getCategory() : "기타";
                categorySpending.merge(category, expense.getAmount(), Integer::sum);
            });

            Map<String, Object> userData = new HashMap<>();
            userData.put("totalSpent", totalSpent);
            userData.put("budget", budget);
            userData.put("categorySpending", categorySpending);

            String strategy = huggingFaceService.generateSpendingStrategy(userData);

            Map<String, String> response = new HashMap<>();
            response.put("strategy", strategy);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("strategy", "소비 전략을 생성할 수 없습니다. 잠시 후 다시 시도해주세요.");
            return ResponseEntity.status(500).body(error);
        }
    }
}
