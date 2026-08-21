package com.taskmanagement.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.taskmanagement.model.AuthProvider;
import com.taskmanagement.model.UserIdentity;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, Long> {
    Optional<UserIdentity> findByProviderAndProviderSubject(
            AuthProvider provider,
            String providerSubject
    );

    Optional<UserIdentity> findByUserIdAndProvider(Long userId, AuthProvider provider);
}
