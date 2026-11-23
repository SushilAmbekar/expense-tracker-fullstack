package com.sushilambekar.expensetracker.analytics;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
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

class BudgetAndAnalyticsIntegrationTest extends IntegrationTest {

    private String token;
    private long food;
    private long transport;
    private long shopping;

    @BeforeEach
    void setUp() throws Exception {
        token = registerUser();
        food = categoryId(token, "Food & Dining");
        transport = categoryId(token, "Transport");
        shopping = categoryId(token, "Shopping");
    }

    @Test
    void aBudgetShowsWhatIsSpentInItsCategory() throws Exception {
        createExpense(token, "750", "2026-10-03", food, "Groceries");
        createExpense(token, "250", "2026-10-20", food, "Dinner");
        createExpense(token, "999", "2026-11-01", food, "Next month");

        setBudget(food, "2026-10", "2000");
        // Setting it again replaces the amount instead of adding a second budget
        setBudget(food, "2026-10", "800");

        mvc.perform(auth(get("/api/budgets"), token).param("month", "2026-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].categoryName").value("Food & Dining"))
                .andExpect(jsonPath("$[0].amount").value(800))
                .andExpect(jsonPath("$[0].spent").value(1000))
                .andExpect(jsonPath("$[0].remaining").value(-200))
                .andExpect(jsonPath("$[0].percentUsed").value(125));
    }

    @Test
    void budgetsCanBeCopiedToTheNextMonthAndDeleted() throws Exception {
        setBudget(food, "2026-09", "5000");
        setBudget(transport, "2026-09", "1500");
        setBudget(transport, "2026-10", "2500");

        String body = mvc.perform(auth(post("/api/budgets/copy"), token).param("from", "2026-09").param("to", "2026-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].categoryName", contains("Food & Dining", "Transport")))
                // The budget October already had is kept
                .andExpect(jsonPath("$[1].amount").value(2500))
                .andReturn().getResponse().getContentAsString();

        long id = read(body).get(0).get("id").asLong();
        mvc.perform(auth(delete("/api/budgets/" + id), token)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/budgets"), token).param("month", "2026-10")).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void budgetValidation() throws Exception {
        mvc.perform(auth(put("/api/budgets"), token).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("categoryId", food, "month", "2026-13", "amount", "100"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.month").value("must be a month such as 2026-11"));
        mvc.perform(auth(get("/api/budgets"), token).param("month", "October")).andExpect(status().isBadRequest());
    }

    @Test
    void monthlySummary() throws Exception {
        createExpense(token, "400", "2026-09-10", food, "September");
        createExpense(token, "300", "2026-10-01", food, "Lunch");
        createExpense(token, "200", "2026-10-01", transport, "Cab");
        createExpense(token, "100", "2026-10-31", food, "Snacks");
        setBudget(shopping, "2026-10", "1000");

        mvc.perform(auth(get("/api/analytics/summary"), token).param("month", "2026-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-10"))
                .andExpect(jsonPath("$.total").value(600))
                .andExpect(jsonPath("$.previousMonthTotal").value(400))
                .andExpect(jsonPath("$.changePercent").value(50.0))
                .andExpect(jsonPath("$.transactionCount").value(3))
                // 600 over the 31 days of October
                .andExpect(jsonPath("$.averagePerDay").value(19.35))
                .andExpect(jsonPath("$.budgetTotal").value(1000))
                .andExpect(jsonPath("$.byCategory[*].name", contains("Food & Dining", "Transport", "Shopping")))
                .andExpect(jsonPath("$.byCategory[0].total").value(400))
                .andExpect(jsonPath("$.byCategory[0].count").value(2))
                .andExpect(jsonPath("$.byCategory[0].percentOfTotal").value(66.7))
                .andExpect(jsonPath("$.byCategory[0].budget", nullValue()))
                .andExpect(jsonPath("$.byCategory[2].total").value(0))
                .andExpect(jsonPath("$.byCategory[2].budget").value(1000))
                .andExpect(jsonPath("$.daily[*].date", contains("2026-10-01", "2026-10-31")))
                .andExpect(jsonPath("$.daily[0].total").value(500));
    }

    @Test
    void summaryOfAMonthWithNoSpendingBefore() throws Exception {
        createExpense(token, "100", "2026-10-05", food, "Tea");

        mvc.perform(auth(get("/api/analytics/summary"), token).param("month", "2026-10"))
                .andExpect(jsonPath("$.previousMonthTotal").value(0))
                .andExpect(jsonPath("$.changePercent", nullValue()));
    }

    @Test
    void trendListsEveryMonthIncludingEmptyOnes() throws Exception {
        createExpense(token, "100", "2026-07-15", food, "July");
        createExpense(token, "50", "2026-07-20", transport, "July");
        createExpense(token, "300", "2026-10-02", food, "October");
        createExpense(token, "999", "2026-04-01", food, "Too early");

        mvc.perform(auth(get("/api/analytics/trend"), token).param("end", "2026-10").param("months", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].month", contains("2026-07", "2026-08", "2026-09", "2026-10")))
                .andExpect(jsonPath("$[0].total").value(150))
                .andExpect(jsonPath("$[1].total").value(0))
                .andExpect(jsonPath("$[3].total").value(300));

        mvc.perform(auth(get("/api/analytics/trend"), token).param("months", "25")).andExpect(status().isBadRequest());
    }

    private void setBudget(long categoryId, String month, String amount) throws Exception {
        mvc.perform(auth(put("/api/budgets"), token).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("categoryId", categoryId, "month", month, "amount", amount))))
                .andExpect(status().isOk());
    }
}
