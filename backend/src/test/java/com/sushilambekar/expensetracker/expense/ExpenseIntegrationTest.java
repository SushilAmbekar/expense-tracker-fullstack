package com.sushilambekar.expensetracker.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sushilambekar.expensetracker.IntegrationTest;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ExpenseIntegrationTest extends IntegrationTest {

    private String token;
    private long food;
    private long transport;

    @BeforeEach
    void setUp() throws Exception {
        token = registerUser();
        food = categoryId(token, "Food & Dining");
        transport = categoryId(token, "Transport");
    }

    @Test
    void createReadUpdateAndDeleteAnExpense() throws Exception {
        long id = createExpense(token, "249.50", "2026-10-05", food, "  Lunch  ");

        mvc.perform(auth(get("/api/expenses/" + id), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(249.50))
                .andExpect(jsonPath("$.date").value("2026-10-05"))
                .andExpect(jsonPath("$.description").value("Lunch"))
                .andExpect(jsonPath("$.paymentMethod").value("UPI"))
                .andExpect(jsonPath("$.category.name").value("Food & Dining"));

        mvc.perform(auth(put("/api/expenses/" + id), token).contentType(MediaType.APPLICATION_JSON)
                        .content(json(expense("300", "2026-10-06", transport, "Cab", "CARD"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(300))
                .andExpect(jsonPath("$.category.name").value("Transport"))
                .andExpect(jsonPath("$.paymentMethod").value("CARD"));

        mvc.perform(auth(delete("/api/expenses/" + id), token)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/expenses/" + id), token)).andExpect(status().isNotFound());
    }

    @Test
    void createReportsInvalidFields() throws Exception {
        Map<String, Object> invalid = new HashMap<>();
        invalid.put("amount", "0");
        invalid.put("categoryId", food);
        invalid.put("description", "x".repeat(256));
        invalid.put("paymentMethod", "UPI");

        mvc.perform(auth(post("/api/expenses"), token).contentType(MediaType.APPLICATION_JSON).content(json(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount", notNullValue()))
                .andExpect(jsonPath("$.errors.date", notNullValue()))
                .andExpect(jsonPath("$.errors.description", notNullValue()));
    }

    @Test
    void anExpenseCannotUseAnotherUsersCategory() throws Exception {
        String otherToken = registerUser();
        long othersCategory = categoryId(otherToken, "Food & Dining");

        mvc.perform(auth(post("/api/expenses"), token).contentType(MediaType.APPLICATION_JSON)
                        .content(json(expense("100", "2026-10-01", othersCategory, "Snacks", "CASH"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Category " + othersCategory + " not found"));
    }

    @Test
    void anotherUsersExpenseLooksLikeItDoesNotExist() throws Exception {
        long id = createExpense(token, "500", "2026-10-01", food, "Groceries");
        String otherToken = registerUser();
        long otherFood = categoryId(otherToken, "Food & Dining");

        mvc.perform(auth(get("/api/expenses/" + id), otherToken)).andExpect(status().isNotFound());
        mvc.perform(auth(put("/api/expenses/" + id), otherToken).contentType(MediaType.APPLICATION_JSON)
                        .content(json(expense("1", "2026-10-01", otherFood, "Hacked", "CASH"))))
                .andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/expenses/" + id), otherToken)).andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/expenses"), otherToken)).andExpect(jsonPath("$.totalElements").value(0));

        mvc.perform(auth(get("/api/expenses/" + id), token)).andExpect(jsonPath("$.description").value("Groceries"));
    }

    @Test
    void listFiltersByDateRangeCategoryAndText() throws Exception {
        createExpense(token, "100", "2026-09-30", food, "Pizza night");
        createExpense(token, "200", "2026-10-01", food, "Pizza lunch");
        createExpense(token, "300", "2026-10-15", transport, "Train");
        createExpense(token, "400", "2026-11-01", food, "Pizza party");

        mvc.perform(auth(get("/api/expenses"), token).param("from", "2026-10-01").param("to", "2026-10-31"))
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(auth(get("/api/expenses"), token).param("categoryId", String.valueOf(food)).param("q", "PIZZA"))
                .andExpect(jsonPath("$.content[*].description",
                        contains("Pizza party", "Pizza lunch", "Pizza night")));
        mvc.perform(auth(get("/api/expenses"), token).param("q", "100%"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listIsPagedAndSorted() throws Exception {
        createExpense(token, "50", "2026-10-03", food, "C");
        createExpense(token, "10", "2026-10-01", food, "A");
        createExpense(token, "30", "2026-10-02", food, "B");

        mvc.perform(auth(get("/api/expenses"), token).param("sortBy", "AMOUNT").param("direction", "ASC")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].description", contains("A", "B")))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        mvc.perform(auth(get("/api/expenses"), token).param("sortBy", "AMOUNT").param("direction", "ASC")
                        .param("size", "2").param("page", "1"))
                .andExpect(jsonPath("$.content[*].description", contains("C")));
    }

    @Test
    void listRejectsInvalidParameters() throws Exception {
        mvc.perform(auth(get("/api/expenses"), token).param("from", "2026-10-31").param("to", "2026-10-01"))
                .andExpect(status().isBadRequest());
        mvc.perform(auth(get("/api/expenses"), token).param("size", "500")).andExpect(status().isBadRequest());
        mvc.perform(auth(get("/api/expenses"), token).param("sortBy", "PASSWORD")).andExpect(status().isBadRequest());
    }

    @Test
    void exportDownloadsTheMatchingExpensesAsCsv() throws Exception {
        createExpense(token, "120.00", "2026-10-02", food, "Tea, biscuits");
        createExpense(token, "80.50", "2026-10-01", transport, "Bus");

        String csv = mvc.perform(auth(get("/api/expenses/export"), token).param("from", "2026-10-01"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"expenses.csv\""))
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andReturn().getResponse().getContentAsString();

        assertThat(csv.split("\r\n")).containsExactly(
                "Date,Category,Description,Payment method,Amount",
                "2026-10-01,Transport,Bus,UPI,80.50",
                "2026-10-02,Food & Dining,\"Tea, biscuits\",UPI,120.00");
    }

    private static Map<String, Object> expense(String amount, String date, long categoryId, String description,
            String paymentMethod) {
        return Map.of("amount", amount, "date", date, "categoryId", categoryId, "description", description,
                "paymentMethod", paymentMethod);
    }
}
