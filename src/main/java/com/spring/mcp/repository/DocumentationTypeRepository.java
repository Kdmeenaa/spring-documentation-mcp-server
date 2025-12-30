package com.spring.mcp.repository;

import com.spring.mcp.model.entity.DocumentationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for DocumentationType entities
 */
@Repository
public interface DocumentationTypeRepository extends JpaRepository<DocumentationType, Long> {

    /**
     * Find documentation type by slug
     */
    Optional<DocumentationType> findBySlug(String slug);

    /**
     * Find documentation type by name
     */
    Optional<DocumentationType> findByName(String name);

    /**
     * Find all documentation types ordered by display order
     */
    List<DocumentationType> findAllByOrderByDisplayOrderAsc();
}
