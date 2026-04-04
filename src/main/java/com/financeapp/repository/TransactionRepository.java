package com.financeapp.repository;

import com.financeapp.dto.response.CategorySummaryResponse;
import com.financeapp.dto.response.MonthlyTrendResponse;
import com.financeapp.enums.TransactionType;
import com.financeapp.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdAndDeletedFalse(Long id);

    long countByDeletedFalse();

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.deleted = false
        AND (:type IS NULL OR t.type = :type)
        AND (:category IS NULL OR LOWER(t.category) LIKE LOWER(CONCAT('%', :category, '%')))
        AND (:startDate IS NULL OR t.date >= :startDate)
        AND (:endDate IS NULL OR t.date <= :endDate)
        """)
    Page<Transaction> findAllWithFilters(
            @Param("type") TransactionType type,
            @Param("category") String category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.type = :type AND t.deleted = false")
    BigDecimal sumByType(@Param("type") TransactionType type);

    @Query("""
        SELECT new com.financeapp.dto.response.CategorySummaryResponse(t.category, SUM(t.amount))
        FROM Transaction t
        WHERE t.deleted = false AND t.type = :type
        GROUP BY t.category
        ORDER BY SUM(t.amount) DESC
        """)
    List<CategorySummaryResponse> getCategoryWiseTotals(@Param("type") TransactionType type);

    @Query("""
        SELECT new com.financeapp.dto.response.MonthlyTrendResponse(
            YEAR(t.date), MONTH(t.date), t.type, SUM(t.amount))
        FROM Transaction t
        WHERE t.deleted = false
        AND t.date >= :startDate
        GROUP BY YEAR(t.date), MONTH(t.date), t.type
        ORDER BY YEAR(t.date), MONTH(t.date)
        """)
    List<MonthlyTrendResponse> getMonthlyTrends(@Param("startDate") LocalDate startDate);

    @Query("SELECT t FROM Transaction t JOIN FETCH t.createdBy WHERE t.deleted = false ORDER BY t.createdAt DESC")
    List<Transaction> findRecentTransactions(Pageable pageable);
}
