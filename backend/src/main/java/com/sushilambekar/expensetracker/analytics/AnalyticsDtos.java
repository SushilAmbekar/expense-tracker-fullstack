package com.sushilambekar.expensetracker.analytics;

import com.sushilambekar.expensetracker.expense.DailyTotal;
import java.math.BigDecimal;
import java.util.List;

public final class AnalyticsDtos {

    private AnalyticsDtos() {
    }

    /**
     * Spending in one month.
     *
     * @param changePercent    change against the previous month, or {@code null} when nothing was spent then
     * @param averagePerDay    total divided by the days of the month so far (the whole month once it is over)
     * @param budgetTotal      sum of the budgets set for the month
     * @param byCategory       categories with spending or a budget, highest spending first
     * @param daily            one entry for each day that has expenses
     */
    public record MonthlySummary(String month, BigDecimal total, BigDecimal previousMonthTotal, Double changePercent,
            long transactionCount, BigDecimal averagePerDay, BigDecimal budgetTotal, List<CategorySpend> byCategory,
            List<DailyTotal> daily) {
    }

    /**
     * @param budget         the budget of the category for the month, or {@code null} when none is set
     * @param percentOfTotal share of the month's spending, rounded to one decimal place
     */
    public record CategorySpend(Long categoryId, String name, String color, BigDecimal total, long count,
            BigDecimal budget, double percentOfTotal) {
    }

    public record MonthlyTotal(String month, BigDecimal total) {
    }
}
