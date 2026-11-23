package com.sushilambekar.expensetracker.budget;

import com.sushilambekar.expensetracker.budget.BudgetDtos.BudgetRequest;
import com.sushilambekar.expensetracker.budget.BudgetDtos.BudgetResponse;
import com.sushilambekar.expensetracker.category.Category;
import com.sushilambekar.expensetracker.category.CategoryRepository;
import com.sushilambekar.expensetracker.common.BadRequestException;
import com.sushilambekar.expensetracker.common.NotFoundException;
import com.sushilambekar.expensetracker.expense.CategoryTotal;
import com.sushilambekar.expensetracker.expense.ExpenseRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetService {

    private final BudgetRepository budgets;
    private final CategoryRepository categories;
    private final ExpenseRepository expenses;

    public BudgetService(BudgetRepository budgets, CategoryRepository categories, ExpenseRepository expenses) {
        this.budgets = budgets;
        this.categories = categories;
        this.expenses = expenses;
    }

    /** The budgets of a month, each with what has been spent in its category so far. */
    @Transactional(readOnly = true)
    public List<BudgetResponse> list(Long userId, YearMonth month) {
        Map<Long, BigDecimal> spent = spentByCategory(userId, month);
        return budgets.findByUserIdAndMonthOrderByCategoryNameAsc(userId, month.toString()).stream()
                .map(budget -> toResponse(budget, spent.getOrDefault(budget.getCategory().getId(), BigDecimal.ZERO)))
                .toList();
    }

    @Transactional
    public BudgetResponse set(Long userId, BudgetRequest request) {
        Category category = categories.findByIdAndUserId(request.categoryId(), userId)
                .orElseThrow(() -> new BadRequestException("Category " + request.categoryId() + " not found"));
        Budget budget = budgets.findByUserIdAndCategoryIdAndMonth(userId, category.getId(), request.month())
                .orElseGet(() -> new Budget(userId, category, request.month(), request.amount()));
        budget.setAmount(request.amount());
        Budget saved = budgets.save(budget);
        BigDecimal spent = spentByCategory(userId, YearMonth.parse(request.month()))
                .getOrDefault(category.getId(), BigDecimal.ZERO);
        return toResponse(saved, spent);
    }

    /** Copies the budgets of one month into another, keeping any the target month already has. */
    @Transactional
    public List<BudgetResponse> copy(Long userId, YearMonth from, YearMonth to) {
        if (from.equals(to)) {
            throw new BadRequestException("'from' and 'to' must be different months");
        }
        for (Budget source : budgets.findByUserIdAndMonthOrderByCategoryNameAsc(userId, from.toString())) {
            if (budgets.findByUserIdAndCategoryIdAndMonth(userId, source.getCategory().getId(), to.toString()).isEmpty()) {
                budgets.save(new Budget(userId, source.getCategory(), to.toString(), source.getAmount()));
            }
        }
        return list(userId, to);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        budgets.delete(budgets.findByIdAndUserId(id, userId).orElseThrow(() -> NotFoundException.of("Budget", id)));
    }

    private Map<Long, BigDecimal> spentByCategory(Long userId, YearMonth month) {
        return expenses.totalsByCategory(userId, month.atDay(1), month.atEndOfMonth()).stream()
                .collect(Collectors.toMap(CategoryTotal::categoryId, CategoryTotal::total));
    }

    static BudgetResponse toResponse(Budget budget, BigDecimal spent) {
        BigDecimal amount = budget.getAmount();
        int percentUsed = spent.multiply(BigDecimal.valueOf(100)).divide(amount, 0, RoundingMode.HALF_UP).intValue();
        Category category = budget.getCategory();
        return new BudgetResponse(budget.getId(), category.getId(), category.getName(), category.getColor(),
                budget.getMonth(), amount, spent, amount.subtract(spent), percentUsed);
    }
}
