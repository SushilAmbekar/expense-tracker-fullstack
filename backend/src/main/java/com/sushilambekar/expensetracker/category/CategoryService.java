package com.sushilambekar.expensetracker.category;

import com.sushilambekar.expensetracker.category.CategoryDtos.CategoryRequest;
import com.sushilambekar.expensetracker.category.CategoryDtos.CategoryResponse;
import com.sushilambekar.expensetracker.common.ConflictException;
import com.sushilambekar.expensetracker.common.NotFoundException;
import com.sushilambekar.expensetracker.expense.ExpenseRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    /** Created for every new user, so the app is usable right after sign-up. */
    static final Map<String, String> DEFAULT_CATEGORIES = Map.of(
            "Food & Dining", "#f97316",
            "Transport", "#0ea5e9",
            "Shopping", "#ec4899",
            "Bills & Utilities", "#6366f1",
            "Health", "#10b981",
            "Entertainment", "#a855f7",
            "Travel", "#14b8a6",
            "Education", "#eab308",
            "Other", "#64748b");

    private final CategoryRepository categories;
    private final ExpenseRepository expenses;

    public CategoryService(CategoryRepository categories, ExpenseRepository expenses) {
        this.categories = categories;
        this.expenses = expenses;
    }

    @Transactional
    public void createDefaults(Long userId) {
        DEFAULT_CATEGORIES.forEach((name, color) -> categories.save(new Category(userId, name, color)));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(Long userId) {
        return categories.findByUserIdOrderByNameAsc(userId).stream().map(CategoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Category getOwned(Long userId, Long id) {
        return categories.findByIdAndUserId(id, userId).orElseThrow(() -> NotFoundException.of("Category", id));
    }

    @Transactional
    public CategoryResponse create(Long userId, CategoryRequest request) {
        String name = request.name().trim();
        if (categories.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new ConflictException("A category named '" + name + "' already exists");
        }
        return CategoryResponse.from(categories.save(new Category(userId, name, request.color().toLowerCase())));
    }

    @Transactional
    public CategoryResponse update(Long userId, Long id, CategoryRequest request) {
        Category category = getOwned(userId, id);
        String name = request.name().trim();
        if (categories.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, name, id)) {
            throw new ConflictException("A category named '" + name + "' already exists");
        }
        category.update(name, request.color().toLowerCase());
        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Category category = getOwned(userId, id);
        long used = expenses.countByUserIdAndCategoryId(userId, id);
        if (used > 0) {
            throw new ConflictException("Category '" + category.getName() + "' has " + used
                    + " expense(s). Move or delete them before deleting the category.");
        }
        categories.delete(category);
    }
}
