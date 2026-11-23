package com.sushilambekar.expensetracker.expense;

import com.sushilambekar.expensetracker.expense.ExpenseDtos.ExpenseFilter;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Builds the WHERE clause for a filtered expense list. Always restricted to one user. */
final class ExpenseSpecifications {

    private ExpenseSpecifications() {
    }

    static Specification<Expense> matching(Long userId, ExpenseFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));
            if (filter.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("expenseDate"), filter.from()));
            }
            if (filter.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("expenseDate"), filter.to()));
            }
            if (filter.categoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), filter.categoryId()));
            }
            if (filter.paymentMethod() != null) {
                predicates.add(cb.equal(root.get("paymentMethod"), filter.paymentMethod()));
            }
            if (filter.q() != null && !filter.q().isBlank()) {
                String pattern = "%" + escapeLike(filter.q().trim().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.like(cb.lower(root.get("description")), pattern, '\\'));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /** Makes % and _ in the search text match literally. */
    static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
