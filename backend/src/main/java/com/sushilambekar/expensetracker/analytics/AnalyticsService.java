package com.sushilambekar.expensetracker.analytics;

import com.sushilambekar.expensetracker.analytics.AnalyticsDtos.CategorySpend;
import com.sushilambekar.expensetracker.analytics.AnalyticsDtos.MonthlySummary;
import com.sushilambekar.expensetracker.analytics.AnalyticsDtos.MonthlyTotal;
import com.sushilambekar.expensetracker.budget.Budget;
import com.sushilambekar.expensetracker.budget.BudgetRepository;
import com.sushilambekar.expensetracker.expense.CategoryTotal;
import com.sushilambekar.expensetracker.expense.ExpenseRepository;
import com.sushilambekar.expensetracker.expense.MonthTotal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ExpenseRepository expenses;
    private final BudgetRepository budgets;
    private final Clock clock;

    public AnalyticsService(ExpenseRepository expenses, BudgetRepository budgets, Clock clock) {
        this.expenses = expenses;
        this.budgets = budgets;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MonthlySummary summary(Long userId, YearMonth month) {
        var from = month.atDay(1);
        var to = month.atEndOfMonth();
        YearMonth previous = month.minusMonths(1);

        BigDecimal total = expenses.sumBetween(userId, from, to);
        BigDecimal previousTotal = expenses.sumBetween(userId, previous.atDay(1), previous.atEndOfMonth());
        long count = expenses.countBetween(userId, from, to);
        List<Budget> monthBudgets = budgets.findByUserIdAndMonthOrderByCategoryNameAsc(userId, month.toString());
        BigDecimal budgetTotal = monthBudgets.stream().map(Budget::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new MonthlySummary(month.toString(), total, previousTotal, changePercent(total, previousTotal), count,
                averagePerDay(total, month), budgetTotal,
                byCategory(expenses.totalsByCategory(userId, from, to), monthBudgets, total),
                expenses.totalsByDay(userId, from, to));
    }

    /** Total spending for each of the {@code months} months ending with {@code end}, oldest first, zero-filled. */
    @Transactional(readOnly = true)
    public List<MonthlyTotal> trend(Long userId, YearMonth end, int months) {
        YearMonth start = end.minusMonths(months - 1L);
        Map<YearMonth, BigDecimal> totals = expenses.totalsByMonth(userId, start.atDay(1), end.atEndOfMonth()).stream()
                .collect(Collectors.toMap(t -> YearMonth.of(t.year(), t.month()), MonthTotal::total));
        List<MonthlyTotal> trend = new ArrayList<>(months);
        for (YearMonth month = start; !month.isAfter(end); month = month.plusMonths(1)) {
            trend.add(new MonthlyTotal(month.toString(), totals.getOrDefault(month, BigDecimal.ZERO)));
        }
        return trend;
    }

    static Double changePercent(BigDecimal current, BigDecimal previous) {
        if (previous.signum() == 0) {
            return null;
        }
        return current.subtract(previous).multiply(HUNDRED).divide(previous, 1, RoundingMode.HALF_UP).doubleValue();
    }

    BigDecimal averagePerDay(BigDecimal total, YearMonth month) {
        YearMonth current = YearMonth.now(clock);
        int days;
        if (month.isAfter(current)) {
            return BigDecimal.ZERO.setScale(2);
        } else if (month.equals(current)) {
            days = LocalDate.now(clock).getDayOfMonth();
        } else {
            days = month.lengthOfMonth();
        }
        return total.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
    }

    private static List<CategorySpend> byCategory(List<CategoryTotal> totals, List<Budget> monthBudgets,
            BigDecimal monthTotal) {
        Map<Long, Budget> budgetByCategory = monthBudgets.stream()
                .collect(Collectors.toMap(b -> b.getCategory().getId(), Function.identity()));
        Map<Long, CategorySpend> result = new LinkedHashMap<>();
        for (CategoryTotal total : totals) {
            Budget budget = budgetByCategory.get(total.categoryId());
            result.put(total.categoryId(), new CategorySpend(total.categoryId(), total.name(), total.color(),
                    total.total(), total.count(), budget != null ? budget.getAmount() : null,
                    percentOf(total.total(), monthTotal)));
        }
        // Categories with a budget but no spending yet still belong on the dashboard.
        for (Budget budget : monthBudgets) {
            var category = budget.getCategory();
            result.putIfAbsent(category.getId(), new CategorySpend(category.getId(), category.getName(),
                    category.getColor(), BigDecimal.ZERO, 0, budget.getAmount(), 0.0));
        }
        return result.values().stream()
                .sorted(Comparator.comparing(CategorySpend::total).reversed().thenComparing(CategorySpend::name))
                .toList();
    }

    private static double percentOf(BigDecimal part, BigDecimal whole) {
        if (whole.signum() == 0) {
            return 0.0;
        }
        return part.multiply(HUNDRED).divide(whole, 1, RoundingMode.HALF_UP).doubleValue();
    }
}
