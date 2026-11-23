package com.sushilambekar.expensetracker.expense;

import com.sushilambekar.expensetracker.category.Category;
import com.sushilambekar.expensetracker.category.CategoryRepository;
import com.sushilambekar.expensetracker.common.BadRequestException;
import com.sushilambekar.expensetracker.common.NotFoundException;
import com.sushilambekar.expensetracker.common.PageResponse;
import com.sushilambekar.expensetracker.expense.ExpenseDtos.ExpenseFilter;
import com.sushilambekar.expensetracker.expense.ExpenseDtos.ExpenseRequest;
import com.sushilambekar.expensetracker.expense.ExpenseDtos.ExpenseResponse;
import com.sushilambekar.expensetracker.expense.ExpenseDtos.SortField;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {

    private final ExpenseRepository expenses;
    private final CategoryRepository categories;
    private final Clock clock;

    public ExpenseService(ExpenseRepository expenses, CategoryRepository categories, Clock clock) {
        this.expenses = expenses;
        this.categories = categories;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> list(Long userId, ExpenseFilter filter, int page, int size,
            SortField sortBy, Sort.Direction direction) {
        validateRange(filter);
        PageRequest pageRequest = PageRequest.of(page, size, sortBy.sort(direction));
        return PageResponse.of(expenses.findAll(ExpenseSpecifications.matching(userId, filter), pageRequest),
                ExpenseResponse::from);
    }

    /** Every matching expense, oldest first, for the CSV export. */
    @Transactional(readOnly = true)
    public List<ExpenseResponse> listAll(Long userId, ExpenseFilter filter) {
        validateRange(filter);
        return expenses.findAll(ExpenseSpecifications.matching(userId, filter), SortField.DATE.sort(Sort.Direction.ASC))
                .stream().map(ExpenseResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ExpenseResponse get(Long userId, Long id) {
        return ExpenseResponse.from(getOwned(userId, id));
    }

    @Transactional
    public ExpenseResponse create(Long userId, ExpenseRequest request) {
        Category category = categoryFor(userId, request.categoryId());
        Expense expense = new Expense(userId, category, request.amount(), request.date(), clean(request.description()),
                request.paymentMethod(), LocalDateTime.now(clock));
        return ExpenseResponse.from(expenses.save(expense));
    }

    @Transactional
    public ExpenseResponse update(Long userId, Long id, ExpenseRequest request) {
        Expense expense = getOwned(userId, id);
        Category category = categoryFor(userId, request.categoryId());
        expense.update(category, request.amount(), request.date(), clean(request.description()),
                request.paymentMethod(), LocalDateTime.now(clock));
        return ExpenseResponse.from(expense);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        expenses.delete(getOwned(userId, id));
    }

    // Another user's expense answers 404, the same as a missing one, so ids reveal nothing.
    private Expense getOwned(Long userId, Long id) {
        return expenses.findByIdAndUserId(id, userId).orElseThrow(() -> NotFoundException.of("Expense", id));
    }

    private Category categoryFor(Long userId, Long categoryId) {
        return categories.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new BadRequestException("Category " + categoryId + " not found"));
    }

    private static void validateRange(ExpenseFilter filter) {
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw new BadRequestException("'from' must be on or before 'to'");
        }
    }

    private static String clean(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }
}
