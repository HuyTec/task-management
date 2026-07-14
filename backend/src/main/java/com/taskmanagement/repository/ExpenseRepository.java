package com.taskmanagement.repository;
import com.taskmanagement.model.Expense;
import com.taskmanagement.model.ExpenseCategory;
import com.taskmanagement.repository.projection.TaskTotalProjection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByUserId(Long userId);

    Optional<Expense> findByIdAndUserId(Long id, Long userId);

    List<Expense> findByTaskId(Long taskId);
    
    @Query("SELECT e FROM Expense e WHERE e.user.id = :userId " +
        "AND (:keyword IS NULL OR LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
        "AND (:minAmount IS NULL OR e.amount >= :minAmount) " +
        "AND (:maxAmount IS NULL OR e.amount <= :maxAmount) " +
        "AND (:fromDate IS NULL OR e.expenseDate >= :fromDate) " +
        "AND (:toDate IS NULL OR e.expenseDate <= :toDate) " +
        "AND (:category IS NULL OR e.category = :category)")
    List<Expense> findByFilter(@Param("userId") Long userId,
                                @Param("minAmount") Double minAmount,
                                @Param("maxAmount") Double maxAmount,
                                @Param("fromDate") LocalDate fromDate,
                                @Param("toDate") LocalDate toDate,
                                @Param("keyword") String keyword,
                                @Param("category") ExpenseCategory category);
    
    @Query("""
    SELECT e.task.id AS taskId, SUM(e.amount) AS total
    FROM Expense e
    WHERE e.task.id IN :taskIds
    GROUP BY e.task.id
    """)
    List<TaskTotalProjection> sumAmountsByTaskIds(@Param("taskIds") List<Long> taskIds);
}