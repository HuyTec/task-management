package com.taskmanagement.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.taskmanagement.service.cache.ExpenseCacheService;
import com.taskmanagement.service.cache.ProjectCacheService;
import com.taskmanagement.service.cache.TaskCacheService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CacheEvictListener {
    private final TaskCacheService taskCacheService;
    private final ExpenseCacheService expenseCacheService;
    private final ProjectCacheService projectCacheService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskEvict(TaskCacheEvictEvent event) {
        taskCacheService.evict(event.userId(), event.taskId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExpenseEvict(ExpenseCacheEvictEvent event) {
        expenseCacheService.evict(event.userId(), event.expenseId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProjectEvict(ProjectCacheEvictEvent event) {
        projectCacheService.evict(event.userId(), event.projectId());
    }
}
