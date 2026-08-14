package com.taskmanagement.repository.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.taskmanagement.dto.task.TaskFilter;
import com.taskmanagement.model.Task;

import jakarta.persistence.criteria.Predicate;

public final class TaskSpecifications {

    private TaskSpecifications() {
    }

    public static Specification<Task> forUser(
            Long userId,
            TaskFilter filter
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Điều kiện bảo mật bắt buộc
            predicates.add(
                    criteriaBuilder.equal(
                            root.get("user").get("id"),
                            userId
                    )
            );

            if (StringUtils.hasText(filter.search())) {
                String keyword = "%" + filter.search().trim().toLowerCase() + "%";

                predicates.add(
                        criteriaBuilder.or(
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(root.get("title")),
                                        keyword
                                ),
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(root.get("description")),
                                        keyword
                                )
                        )
                );
            }

            if (filter.status() != null) {
                predicates.add(
                        criteriaBuilder.equal(root.get("status"), filter.status())
                );
            }

            if (filter.priority() != null) {
                predicates.add(
                        criteriaBuilder.equal(root.get("priority"), filter.priority())
                );
            }

            if (filter.projectId() != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("project").get("id"),
                                filter.projectId()
                        )
                );
            }

            if (filter.dueFrom() != null) {
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("dueDate"),
                                filter.dueFrom()
                        )
                );
            }

            if (filter.dueTo() != null) {
                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(
                                root.get("dueDate"),
                                filter.dueTo()
                        )
                );
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}