package com.sushilambekar.expensetracker.category;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sushilambekar.expensetracker.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class CategoryIntegrationTest extends IntegrationTest {

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = registerUser();
    }

    @Test
    void createAndRenameACategory() throws Exception {
        String body = mvc.perform(auth(post("/api/categories"), token).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", " Pets ", "color", "#AABBCC"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Pets"))
                .andExpect(jsonPath("$.color").value("#aabbcc"))
                .andReturn().getResponse().getContentAsString();
        long id = read(body).get("id").asLong();

        mvc.perform(auth(put("/api/categories/" + id), token).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Pet care", "color", "#112233"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pet care"));
        mvc.perform(auth(get("/api/categories"), token)).andExpect(jsonPath("$[*].name", hasItem("Pet care")));
    }

    @Test
    void namesAreUniquePerUserIgnoringCase() throws Exception {
        mvc.perform(auth(post("/api/categories"), token).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "transport", "color", "#000000"))))
                .andExpect(status().isConflict());

        long shopping = categoryId(token, "Shopping");
        mvc.perform(auth(put("/api/categories/" + shopping), token).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Travel", "color", "#000000"))))
                .andExpect(status().isConflict());

        // Another user can use the same name
        String otherToken = registerUser();
        mvc.perform(auth(post("/api/categories"), otherToken).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Pets", "color", "#000000"))))
                .andExpect(status().isCreated());
    }

    @Test
    void colourMustBeAHexColour() throws Exception {
        mvc.perform(auth(post("/api/categories"), token).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Gifts", "color", "red"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.color").value("must be a hex colour such as #4f46e5"));
    }

    @Test
    void aCategoryWithExpensesCannotBeDeleted() throws Exception {
        long food = categoryId(token, "Food & Dining");
        long id = createExpense(token, "99", "2026-10-01", food, "Snacks");

        mvc.perform(auth(delete("/api/categories/" + food), token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Category 'Food & Dining' has 1 expense(s). Move or delete them before deleting the category."));

        mvc.perform(auth(delete("/api/expenses/" + id), token)).andExpect(status().isNoContent());
        mvc.perform(auth(delete("/api/categories/" + food), token)).andExpect(status().isNoContent());
    }

    @Test
    void anotherUsersCategoryIsNotFound() throws Exception {
        long food = categoryId(token, "Food & Dining");
        String otherToken = registerUser();

        mvc.perform(auth(put("/api/categories/" + food), otherToken).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Mine now", "color", "#000000"))))
                .andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/categories/" + food), otherToken)).andExpect(status().isNotFound());
    }
}
