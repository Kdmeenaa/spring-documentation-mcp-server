package com.spring.mcp.repository;

import com.spring.mcp.model.entity.ProjectVersion;
import com.spring.mcp.model.entity.SpringBootCompatibility;
import com.spring.mcp.model.entity.SpringBootVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Spring Boot compatibility mappings.
 * Provides methods to query which Spring project versions are compatible with specific Spring Boot versions.
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-08
 */
@Repository
public interface SpringBootCompatibilityRepository extends JpaRepository<SpringBootCompatibility, Long> {

    /**
     * Find all compatible project versions for a given Spring Boot version.
     *
     * @param springBootVersion the Spring Boot version
     * @return list of compatibility mappings
     */
    List<SpringBootCompatibility> findBySpringBootVersion(SpringBootVersion springBootVersion);

    /**
     * Find all Spring Boot versions that are compatible with a specific project version.
     *
     * @param compatibleProjectVersion the project version
     * @return list of compatibility mappings
     */
    List<SpringBootCompatibility> findByCompatibleProjectVersion(ProjectVersion compatibleProjectVersion);

    /**
     * Check if a compatibility mapping already exists.
     *
     * @param springBootVersion the Spring Boot version (from spring_boot_versions table)
     * @param compatibleProjectVersion the compatible project version (from project_versions table)
     * @return true if the mapping exists
     */
    boolean existsBySpringBootVersionAndCompatibleProjectVersion(
            SpringBootVersion springBootVersion,
            ProjectVersion compatibleProjectVersion
    );

    /**
     * Delete all compatibility mappings for a specific Spring Boot version.
     *
     * @param springBootVersion the Spring Boot version
     */
    void deleteBySpringBootVersion(SpringBootVersion springBootVersion);

    /**
     * Find all compatible project versions for a Spring Boot version, grouped by project.
     *
     * @param springBootVersionId the Spring Boot version ID
     * @return list of compatibility mappings with project details
     */
    @Query("""
        SELECT sbc
        FROM SpringBootCompatibility sbc
        JOIN FETCH sbc.compatibleProjectVersion cpv
        JOIN FETCH cpv.project p
        WHERE sbc.springBootVersion.id = :springBootVersionId
        ORDER BY p.name, cpv.majorVersion DESC, cpv.minorVersion DESC, cpv.patchVersion DESC
        """)
    List<SpringBootCompatibility> findAllBySpringBootVersionIdWithProjectDetails(
            @Param("springBootVersionId") Long springBootVersionId
    );
}
