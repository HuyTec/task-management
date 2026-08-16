package com.taskmanagement.utils;

import java.util.Set;

import org.springframework.data.domain.Pageable;

import com.taskmanagement.exception.BadRequestException;

public final class PageableValidator {

    private PageableValidator() {
    }

    public static void requireAllowedSorts(Pageable pageable, Set<String> allowedProperties) {
        pageable.getSort().forEach(order -> {
            if (!allowedProperties.contains(order.getProperty())) {
                throw new BadRequestException("Unsupported sort property: " + order.getProperty());
            }
        });
    }
}
