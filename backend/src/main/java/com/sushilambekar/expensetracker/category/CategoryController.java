package com.sushilambekar.expensetracker.category;

import com.sushilambekar.expensetracker.category.CategoryDtos.CategoryRequest;
import com.sushilambekar.expensetracker.category.CategoryDtos.CategoryResponse;
import com.sushilambekar.expensetracker.security.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List your categories")
    public List<CategoryResponse> list(@CurrentUserId Long userId) {
        return service.list(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a category")
    public CategoryResponse create(@CurrentUserId Long userId, @Valid @RequestBody CategoryRequest request) {
        return service.create(userId, request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Rename or recolour a category")
    public CategoryResponse update(@CurrentUserId Long userId, @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        return service.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a category that has no expenses")
    public void delete(@CurrentUserId Long userId, @PathVariable Long id) {
        service.delete(userId, id);
    }
}
