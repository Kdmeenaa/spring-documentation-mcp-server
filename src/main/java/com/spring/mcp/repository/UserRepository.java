package com.spring.mcp.repository;

import com.spring.mcp.model.entity.User;
import com.spring.mcp.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for User entities
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Find all enabled users
     */
    List<User> findByEnabledTrue();

    /**
     * Find users by role
     */
    List<User> findByRole(UserRole role);

    /**
     * Find enabled users by role
     */
    List<User> findByRoleAndEnabledTrue(UserRole role);

    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Count users by role
     */
    long countByRole(UserRole role);
}
