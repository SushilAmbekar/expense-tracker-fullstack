package com.sushilambekar.expensetracker;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Runs the whole application (security, validation, JPA, Flyway) against an in-memory database.
 * Each test registers its own users, so tests never see each other's data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTest {

    private static final AtomicInteger USERS = new AtomicInteger();

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /** Registers a new user and returns the access token. */
    protected String registerUser() throws Exception {
        String email = "user" + USERS.incrementAndGet() + "@test.dev";
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Test User", "email", email, "password", "password123"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    protected long categoryId(String token, String name) throws Exception {
        for (JsonNode category : read(mvc.perform(auth(get("/api/categories"), token)).andReturn().getResponse()
                .getContentAsString())) {
            if (category.get("name").asText().equals(name)) {
                return category.get("id").asLong();
            }
        }
        throw new AssertionError("No category named " + name);
    }

    protected long createExpense(String token, String amount, String date, long categoryId, String description)
            throws Exception {
        String body = mvc.perform(auth(post("/api/expenses"), token).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("amount", amount, "date", date, "categoryId", categoryId,
                                "description", description, "paymentMethod", "UPI"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    protected static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String token) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    protected JsonNode read(String json) throws Exception {
        return objectMapper.readTree(json);
    }
}
