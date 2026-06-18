package com.taskmanagement.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import jakarta.persistence.PrePersist;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "expenses")
public class Expense {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Column(nullable = false, length = 1000)
    @NotBlank(message = "Description cannot be blank")
    private String description;

    @Column(nullable = false)
    @Positive(message = "Amount must be positive")
    private Double amount;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "task_id")
    private Task task;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private ExpenseCategory expenseCategory;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;
    
    // Getters and Setters
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    } 

}
