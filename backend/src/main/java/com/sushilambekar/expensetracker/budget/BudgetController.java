package com.sushilambekar.expensetracker.budget;

import com.sushilambekar.expensetracker.budget.BudgetDtos.BudgetRequest;
import com.sushilambekar.expensetracker.budget.BudgetDtos.BudgetResponse;
import com.sushilambekar.expensetracker.security.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.YearMonth;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/budgets")
@Tag(name = "Budgets")
public class BudgetController {

    private final BudgetService service;
    private final Clock clock;

    public BudgetController(BudgetService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @GetMapping
    @Operation(summary = "Budgets of a month (default: this month) with the amount spent so far")
    public List<BudgetResponse> list(@CurrentUserId Long userId, @RequestParam(required = false) YearMonth month) {
        return service.list(userId, month != null ? month : YearMonth.now(clock));
    }

    @PutMapping
    @Operation(summary = "Set the budget of a category for a month")
    public BudgetResponse set(@CurrentUserId Long userId, @Valid @RequestBody BudgetRequest request) {
        return service.set(userId, request);
    }

    @PostMapping("/copy")
    @Operation(summary = "Copy the budgets of one month into another")
    public List<BudgetResponse> copy(@CurrentUserId Long userId, @RequestParam YearMonth from,
            @RequestParam YearMonth to) {
        return service.copy(userId, from, to);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a budget")
    public void delete(@CurrentUserId Long userId, @PathVariable Long id) {
        service.delete(userId, id);
    }
}
