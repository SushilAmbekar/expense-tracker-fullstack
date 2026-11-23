package com.sushilambekar.expensetracker.budget;

import com.sushilambekar.expensetracker.category.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** A spending limit for one category in one month. */
@Entity
@Table(name = "budgets")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /** The month as {@code yyyy-MM}. */
    @Column(name = "budget_month", nullable = false, length = 7)
    private String month;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    protected Budget() {
    }

    public Budget(Long userId, Category category, String month, BigDecimal amount) {
        this.userId = userId;
        this.category = category;
        this.month = month;
        this.amount = amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Category getCategory() {
        return category;
    }

    public String getMonth() {
        return month;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
