package com.spring.mcp.repository;

import com.spring.mcp.model.entity.SpringProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SpringProject entities
 */
@Repository
public interface SpringProjectRepository extends JpaRepository<SpringProject, Long> {

    /**
     * Find a project by its slug
     */
    Optional<SpringProject> findBySlug(String slug);

    /**
     * Find a project by its name
     */
    Optional<SpringProject> findByName(String name);

    /**
     * Find all active projects
     */
    List<SpringProject> findByActiveTrue();

    /**
     * Find all active projects ordered by name
     */
    @Query("SELECT p FROM SpringProject p WHERE p.active = true ORDER BY p.name")
    List<SpringProject> findAllActiveOrderByName();

    /**
     * Check if a project exists by name
     */
    boolean existsByName(String name);

    /**
     * Check if a project exists by slug
     */
    boolean existsBySlug(String slug);

    /**
     * Find all projects compatible with a specific Spring Boot version.
     * Uses the spring_boot_compatibility table to determine compatibility.
     *
     * @param springBootVersionId the Spring Boot version ID
     * @return list of compatible projects, ordered by name
     */
    @Query("""
        SELECT DISTINCT p
        FROM SpringProject p
        JOIN p.versions pv
        JOIN SpringBootCompatibility sbc ON sbc.compatibleProjectVersion = pv
        WHERE sbc.springBootVersion.id = :springBootVersionId
        ORDER BY p.name
        """)
    List<SpringProject> findAllCompatibleWithSpringBootVersion(@Param("springBootVersionId") Long springBootVersionId);
}
