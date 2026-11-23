package com.sushilambekar.expensetracker.demo;

import com.sushilambekar.expensetracker.budget.Budget;
import com.sushilambekar.expensetracker.budget.BudgetRepository;
import com.sushilambekar.expensetracker.category.Category;
import com.sushilambekar.expensetracker.category.CategoryRepository;
import com.sushilambekar.expensetracker.category.CategoryService;
import com.sushilambekar.expensetracker.expense.Expense;
import com.sushilambekar.expensetracker.expense.ExpenseRepository;
import com.sushilambekar.expensetracker.expense.PaymentMethod;
import com.sushilambekar.expensetracker.user.User;
import com.sushilambekar.expensetracker.user.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates a demo account with three months of realistic expenses and this month's budgets, so the app can be
 * explored right after {@code docker compose up}. Enabled with {@code DEMO_DATA_ENABLED=true}; runs once.
 */
@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataLoader implements ApplicationRunner {

    public static final String DEMO_EMAIL = "demo@expense-tracker.dev";
    public static final String DEMO_PASSWORD = "demo12345";

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    /** Category, description, typical amount, payment method. */
    private record Template(String category, String description, int amount, PaymentMethod method) {
    }

    private static final List<Template> TEMPLATES = List.of(
            new Template("Food & Dining", "Groceries", 1800, PaymentMethod.UPI),
            new Template("Food & Dining", "Lunch with team", 450, PaymentMethod.UPI),
            new Template("Food & Dining", "Dinner order", 620, PaymentMethod.CARD),
            new Template("Food & Dining", "Coffee", 180, PaymentMethod.UPI),
            new Template("Transport", "Metro card recharge", 500, PaymentMethod.UPI),
            new Template("Transport", "Cab to office", 320, PaymentMethod.UPI),
            new Template("Transport", "Fuel", 2000, PaymentMethod.CARD),
            new Template("Shopping", "Clothes", 2400, PaymentMethod.CARD),
            new Template("Shopping", "Headphones", 3200, PaymentMethod.CARD),
            new Template("Bills & Utilities", "Electricity bill", 1650, PaymentMethod.NET_BANKING),
            new Template("Bills & Utilities", "Mobile recharge", 299, PaymentMethod.UPI),
            new Template("Bills & Utilities", "Internet", 799, PaymentMethod.NET_BANKING),
            new Template("Health", "Pharmacy", 540, PaymentMethod.UPI),
            new Template("Health", "Gym membership", 1500, PaymentMethod.CARD),
            new Template("Entertainment", "Movie tickets", 700, PaymentMethod.UPI),
            new Template("Entertainment", "Streaming subscription", 499, PaymentMethod.CARD),
            new Template("Education", "Online course", 1299, PaymentMethod.CARD),
            new Template("Travel", "Weekend trip", 4500, PaymentMethod.CARD));

    private final UserRepository users;
    private final CategoryRepository categories;
    private final CategoryService categoryService;
    private final ExpenseRepository expenses;
    private final BudgetRepository budgets;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DemoDataLoader(UserRepository users, CategoryRepository categories, CategoryService categoryService,
            ExpenseRepository expenses, BudgetRepository budgets, PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.categories = categories;
        this.categoryService = categoryService;
        this.expenses = expenses;
        this.budgets = budgets;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.existsByEmail(DEMO_EMAIL)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        User user = users.save(new User("Demo User", DEMO_EMAIL, passwordEncoder.encode(DEMO_PASSWORD), now));
        categoryService.createDefaults(user.getId());
        Map<String, Category> byName = categories.findByUserIdOrderByNameAsc(user.getId()).stream()
                .collect(Collectors.toMap(Category::getName, Function.identity()));

        Random random = new Random(42);
        LocalDate today = LocalDate.now(clock);
        LocalDate start = YearMonth.from(today).minusMonths(2).atDay(1);
        for (LocalDate day = start; !day.isAfter(today); day = day.plusDays(1)) {
            int perDay = random.nextInt(3);
            for (int i = 0; i < perDay; i++) {
                Template template = TEMPLATES.get(random.nextInt(TEMPLATES.size()));
                // Up to 25% either side of the typical amount
                double factor = 0.75 + random.nextDouble() * 0.5;
                BigDecimal amount = BigDecimal.valueOf(Math.round(template.amount() * factor));
                expenses.save(new Expense(user.getId(), byName.get(template.category()), amount, day,
                        template.description(), template.method(), now));
            }
        }

        String month = YearMonth.from(today).toString();
        Map.of("Food & Dining", 9000, "Transport", 4000, "Shopping", 5000, "Bills & Utilities", 3500,
                        "Entertainment", 2000)
                .forEach((name, amount) -> budgets.save(
                        new Budget(user.getId(), byName.get(name), month, BigDecimal.valueOf(amount))));
        log.info("Created the demo account {} with sample expenses", DEMO_EMAIL);
    }
}
