package com.sushilambekar.expensetracker.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    // The category is loaded in the same query, so listing a page does not run one query per row.
    @Override
    @EntityGraph(attributePaths = "category")
    Page<Expense> findAll(Specification<Expense> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "category")
    List<Expense> findAll(Specification<Expense> spec, Sort sort);

    @EntityGraph(attributePaths = "category")
    Optional<Expense> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndCategoryId(Long userId, Long categoryId);

    @Query("""
            select coalesce(sum(e.amount), 0) from Expense e
            where e.userId = :userId and e.expenseDate between :from and :to""")
    BigDecimal sumBetween(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select count(e) from Expense e
            where e.userId = :userId and e.expenseDate between :from and :to""")
    long countBetween(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select new com.sushilambekar.expensetracker.expense.CategoryTotal(c.id, c.name, c.color, sum(e.amount), count(e))
            from Expense e join e.category c
            where e.userId = :userId and e.expenseDate between :from and :to
            group by c.id, c.name, c.color
            order by sum(e.amount) desc""")
    List<CategoryTotal> totalsByCategory(@Param("userId") Long userId, @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            select new com.sushilambekar.expensetracker.expense.DailyTotal(e.expenseDate, sum(e.amount))
            from Expense e
            where e.userId = :userId and e.expenseDate between :from and :to
            group by e.expenseDate
            order by e.expenseDate""")
    List<DailyTotal> totalsByDay(@Param("userId") Long userId, @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            select new com.sushilambekar.expensetracker.expense.MonthTotal(
                extract(year from e.expenseDate), extract(month from e.expenseDate), sum(e.amount))
            from Expense e
            where e.userId = :userId and e.expenseDate between :from and :to
            group by extract(year from e.expenseDate), extract(month from e.expenseDate)""")
    List<MonthTotal> totalsByMonth(@Param("userId") Long userId, @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
