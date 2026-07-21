package com.mbanni.shop.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByRole(String role);
    Optional<User> findById(String id);

    boolean existsByEmail(String email);
}

