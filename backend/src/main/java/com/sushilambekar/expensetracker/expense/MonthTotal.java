package com.sushilambekar.expensetracker.expense;

import java.math.BigDecimal;

/** Total spent in one calendar month (a row of a GROUP BY query). */
public record MonthTotal(Integer year, Integer month, BigDecimal total) {
}
