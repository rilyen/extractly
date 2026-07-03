package com._6.extractly.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com._6.extractly.models.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
