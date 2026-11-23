package com.sushilambekar.expensetracker.expense;

import com.sushilambekar.expensetracker.category.CategoryDtos.CategoryResponse;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

public final class ExpenseDtos {

    private ExpenseDtos() {
    }

    public record ExpenseRequest(
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
            @NotNull LocalDate date,
            @NotNull Long categoryId,
            @Size(max = 255) String description,
            @NotNull PaymentMethod paymentMethod) {
    }

    public record ExpenseResponse(Long id, BigDecimal amount, LocalDate date, String description,
            PaymentMethod paymentMethod, CategoryResponse category) {

        public static ExpenseResponse from(Expense expense) {
            return new ExpenseResponse(expense.getId(), expense.getAmount(), expense.getExpenseDate(),
                    expense.getDescription(), expense.getPaymentMethod(), CategoryResponse.from(expense.getCategory()));
        }
    }

    /** Optional filters for listing and exporting expenses. Every filter that is set must match. */
    public record ExpenseFilter(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Long categoryId,
            PaymentMethod paymentMethod,
            /** Text to look for in the description (case-insensitive) */
            String q) {
    }

    /** The columns a list can be sorted by. */
    public enum SortField {
        DATE("expenseDate"),
        AMOUNT("amount"),
        DESCRIPTION("description"),
        CREATED("createdAt");

        private final String property;

        SortField(String property) {
            this.property = property;
        }

        public Sort sort(Sort.Direction direction) {
            // The id breaks ties, so paging through equal values is stable.
            return Sort.by(direction, property).and(Sort.by(Sort.Direction.DESC, "id"));
        }
    }
}
