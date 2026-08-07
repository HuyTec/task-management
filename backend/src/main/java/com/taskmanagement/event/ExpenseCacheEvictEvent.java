package com.taskmanagement.event;

public record ExpenseCacheEvictEvent(Long userId, Long expenseId) {
}
