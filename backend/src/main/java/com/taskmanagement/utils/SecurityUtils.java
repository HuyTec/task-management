package com.taskmanagement.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.taskmanagement.security.CustomUserDetails;

@Component
public class SecurityUtils {

    public CustomUserDetails getCurrentUser() {
        Authentication authentication = getAuthentication();
        return (CustomUserDetails) authentication.getPrincipal();
    }

    public Authentication getAuthentication(){
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public boolean isAdmin(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return isAdmin;
    }
}
