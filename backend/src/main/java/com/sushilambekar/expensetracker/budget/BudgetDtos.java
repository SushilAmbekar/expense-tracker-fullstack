package com.sushilambekar.expensetracker.budget;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public final class BudgetDtos {

    private BudgetDtos() {
    }

    /** Sets the budget for a category and month, creating it or replacing the amount. */
    public record BudgetRequest(
            @NotNull Long categoryId,
            @NotNull @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "must be a month such as 2026-11") String month,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount) {
    }

    /**
     * @param percentUsed how much of the budget is spent, rounded; above 100 when over budget
     */
    public record BudgetResponse(Long id, Long categoryId, String categoryName, String color, String month,
            BigDecimal amount, BigDecimal spent, BigDecimal remaining, int percentUsed) {
    }
}
