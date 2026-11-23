package com.sushilambekar.expensetracker.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.sushilambekar.expensetracker.budget.BudgetRepository;
import com.sushilambekar.expensetracker.expense.ExpenseRepository;
import com.sushilambekar.expensetracker.expense.MonthTotal;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    /** "Now" is 10 November 2026. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-10T09:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ExpenseRepository expenses;

    @Mock
    private BudgetRepository budgets;

    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsService(expenses, budgets, CLOCK);
    }

    @Test
    void changePercentIsRoundedToOneDecimal() {
        assertThat(AnalyticsService.changePercent(new BigDecimal("150"), new BigDecimal("120"))).isEqualTo(25.0);
        assertThat(AnalyticsService.changePercent(new BigDecimal("100"), new BigDecimal("300"))).isEqualTo(-66.7);
    }

    @Test
    void changePercentIsUnknownWhenNothingWasSpentTheMonthBefore() {
        assertThat(AnalyticsService.changePercent(new BigDecimal("150"), BigDecimal.ZERO)).isNull();
    }

    @Test
    void averagePerDayOfTheCurrentMonthCountsOnlyTheDaysSoFar() {
        assertThat(service.averagePerDay(new BigDecimal("1000"), YearMonth.of(2026, 11))).isEqualByComparingTo("100.00");
    }

    @Test
    void averagePerDayOfAPastMonthUsesAllItsDays() {
        assertThat(service.averagePerDay(new BigDecimal("3000"), YearMonth.of(2026, 9))).isEqualByComparingTo("100.00");
    }

    @Test
    void averagePerDayOfAFutureMonthIsZero() {
        assertThat(service.averagePerDay(new BigDecimal("500"), YearMonth.of(2026, 12))).isEqualByComparingTo("0");
    }

    @Test
    void trendFillsMonthsWithoutExpensesWithZero() {
        when(expenses.totalsByMonth(eq(7L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(new MonthTotal(2026, 9, new BigDecimal("1200")),
                        new MonthTotal(2026, 11, new BigDecimal("300"))));

        var trend = service.trend(7L, YearMonth.of(2026, 11), 3);

        assertThat(trend).extracting(AnalyticsDtos.MonthlyTotal::month).containsExactly("2026-09", "2026-10", "2026-11");
        assertThat(trend).extracting(AnalyticsDtos.MonthlyTotal::total)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("1200"), BigDecimal.ZERO, new BigDecimal("300"));
    }
}
