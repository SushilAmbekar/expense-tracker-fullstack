package com.sushilambekar.expensetracker.expense;

import java.math.BigDecimal;

/** Total spent in one category over a date range (a row of a GROUP BY query). */
public record CategoryTotal(Long categoryId, String name, String color, BigDecimal total, Long count) {
}
