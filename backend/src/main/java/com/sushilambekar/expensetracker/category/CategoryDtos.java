package com.sushilambekar.expensetracker.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class CategoryDtos {

    private CategoryDtos() {
    }

    public record CategoryRequest(
            @NotBlank @Size(max = 50) String name,
            @NotBlank @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "must be a hex colour such as #4f46e5") String color) {
    }

    public record CategoryResponse(Long id, String name, String color) {

        public static CategoryResponse from(Category category) {
            return new CategoryResponse(category.getId(), category.getName(), category.getColor());
        }
    }
}
