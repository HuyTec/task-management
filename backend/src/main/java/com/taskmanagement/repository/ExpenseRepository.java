package com.taskmanagement.repository;
import com.taskmanagement.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByUserId(Long userId);
    
    @Query("SELECT e FROM Expense e WHERE e.user.id = :userId" 
    + " AND (:keyword IS NULL OR LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')))"
    + " AND (:minAmount IS NULL OR e.amount >= :minAmount)"
    + " AND (:maxAmount IS NULL OR e.amount <= :maxAmount)"
    + " AND (:fromDate IS NULL OR e.expenseDate >= :fromDate)"
    + " AND (:toDate IS NULL OR e.expenseDate <= :toDate)"
    + " AND (:categoryId IS NULL OR e.expenseCategory.id = :categoryId)")
    List<Expense> findByFilter(@Param("userId") Long userId, 
                            @Param("minAmount") Double minAmount,
                            @Param("maxAmount") Double maxAmount, 
                            @Param("fromDate") LocalDate fromDate,
                            @Param("toDate") LocalDate toDate,
                            @Param("keyword") String keyword,
                            @Param("categoryId") Long categoryId);
}