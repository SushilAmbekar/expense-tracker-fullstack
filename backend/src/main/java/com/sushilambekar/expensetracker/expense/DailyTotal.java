package com.sushilambekar.expensetracker.expense;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Total spent on one day (a row of a GROUP BY query). */
public record DailyTotal(LocalDate date, BigDecimal total) {
}
