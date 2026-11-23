package com.sushilambekar.expensetracker.analytics;

import com.sushilambekar.expensetracker.analytics.AnalyticsDtos.MonthlySummary;
import com.sushilambekar.expensetracker.analytics.AnalyticsDtos.MonthlyTotal;
import com.sushilambekar.expensetracker.security.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Clock;
import java.time.YearMonth;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics")
public class AnalyticsController {

    private final AnalyticsService service;
    private final Clock clock;

    public AnalyticsController(AnalyticsService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @GetMapping("/summary")
    @Operation(summary = "Spending summary of a month (default: this month)")
    public MonthlySummary summary(@CurrentUserId Long userId, @RequestParam(required = false) YearMonth month) {
        return service.summary(userId, month != null ? month : YearMonth.now(clock));
    }

    @GetMapping("/trend")
    @Operation(summary = "Total spending per month, for the months ending with 'end' (default: this month)")
    public List<MonthlyTotal> trend(@CurrentUserId Long userId, @RequestParam(required = false) YearMonth end,
            @RequestParam(defaultValue = "6") @Min(1) @Max(24) int months) {
        return service.trend(userId, end != null ? end : YearMonth.now(clock), months);
    }
}
