package com.sushilambekar.expensetracker.expense;

import com.sushilambekar.expensetracker.common.PageResponse;
import com.sushilambekar.expensetracker.expense.ExpenseDtos.ExpenseFilter;
import com.sushilambekar.expensetracker.expense.ExpenseDtos.ExpenseRequest;
import com.sushilambekar.expensetracker.expense.ExpenseDtos.ExpenseResponse;
import com.sushilambekar.expensetracker.expense.ExpenseDtos.SortField;
import com.sushilambekar.expensetracker.security.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.nio.charset.StandardCharsets;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/expenses")
@Tag(name = "Expenses")
public class ExpenseController {

    private static final MediaType TEXT_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final ExpenseService service;

    public ExpenseController(ExpenseService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List expenses, filtered, sorted and paged")
    public PageResponse<ExpenseResponse> list(@CurrentUserId Long userId, @ParameterObject ExpenseFilter filter,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "DATE") SortField sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        return service.list(userId, filter, page, size, sortBy, direction);
    }

    @GetMapping("/export")
    @Operation(summary = "Download the matching expenses as CSV")
    public ResponseEntity<String> export(@CurrentUserId Long userId, @ParameterObject ExpenseFilter filter) {
        String csv = ExpenseCsvWriter.write(service.listAll(userId, filter));
        ContentDisposition disposition = ContentDisposition.attachment().filename("expenses.csv").build();
        return ResponseEntity.ok()
                .contentType(TEXT_CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(csv);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one expense")
    public ExpenseResponse get(@CurrentUserId Long userId, @PathVariable Long id) {
        return service.get(userId, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add an expense")
    public ExpenseResponse create(@CurrentUserId Long userId, @Valid @RequestBody ExpenseRequest request) {
        return service.create(userId, request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an expense")
    public ExpenseResponse update(@CurrentUserId Long userId, @PathVariable Long id,
            @Valid @RequestBody ExpenseRequest request) {
        return service.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an expense")
    public void delete(@CurrentUserId Long userId, @PathVariable Long id) {
        service.delete(userId, id);
    }
}
