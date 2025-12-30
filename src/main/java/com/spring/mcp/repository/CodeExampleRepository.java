package com.spring.mcp.repository;

import com.spring.mcp.model.entity.CodeExample;
import com.spring.mcp.model.entity.ProjectVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for CodeExample entities
 */
@Repository
public interface CodeExampleRepository extends JpaRepository<CodeExample, Long> {

    /**
     * Find all examples for a specific version
     */
    List<CodeExample> findByVersion(ProjectVersion version);

    /**
     * Find examples by version and language
     */
    List<CodeExample> findByVersionAndLanguage(ProjectVersion version, String language);

    /**
     * Find examples by version and category
     */
    List<CodeExample> findByVersionAndCategory(ProjectVersion version, String category);

    /**
     * Search examples by title
     */
    @Query("SELECT c FROM CodeExample c WHERE LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<CodeExample> searchByTitle(@Param("query") String query);

    /**
     * Find examples containing specific tags
     * Note: Uses PostgreSQL array operator
     */
    @Query(value = "SELECT * FROM code_examples WHERE tags && CAST(:tags AS text[])",
           nativeQuery = true)
    List<CodeExample> findByTagsContaining(@Param("tags") String[] tags);

    /**
     * Count examples by version
     */
    long countByVersion(ProjectVersion version);

    /**
     * Find distinct languages used in examples
     */
    @Query("SELECT DISTINCT c.language FROM CodeExample c ORDER BY c.language")
    List<String> findDistinctLanguages();

    /**
     * Find distinct categories
     */
    @Query("SELECT DISTINCT c.category FROM CodeExample c WHERE c.category IS NOT NULL ORDER BY c.category")
    List<String> findDistinctCategories();

    /**
     * Find examples by version ID
     */
    List<CodeExample> findByVersionId(Long versionId);

    /**
     * Find examples by project slug
     */
    @Query("SELECT c FROM CodeExample c JOIN c.version v JOIN v.project p WHERE p.slug = :slug")
    List<CodeExample> findByVersionProjectSlug(@Param("slug") String slug);

    /**
     * Find examples by project slug and version string
     */
    @Query("SELECT c FROM CodeExample c JOIN c.version v JOIN v.project p WHERE p.slug = :slug AND v.version = :version")
    List<CodeExample> findByVersionProjectSlugAndVersionVersion(@Param("slug") String slug, @Param("version") String version);

    /**
     * Advanced filtering with project, version, category, search text
     * Search includes example title, description, project name, and project slug
     */
    @Query(value = """
        SELECT ce.* FROM code_examples ce
        JOIN project_versions pv ON ce.version_id = pv.id
        JOIN spring_projects sp ON pv.project_id = sp.id
        WHERE (:projectSlug IS NULL OR sp.slug = :projectSlug)
        AND (:versionStr IS NULL OR pv.version = :versionStr)
        AND (:category IS NULL OR ce.category = :category)
        AND (:searchText IS NULL OR
             LOWER(ce.title::text) LIKE LOWER(CONCAT('%', :searchText, '%')) OR
             LOWER(ce.description::text) LIKE LOWER(CONCAT('%', :searchText, '%')) OR
             LOWER(sp.name::text) LIKE LOWER(CONCAT('%', :searchText, '%')) OR
             LOWER(sp.slug::text) LIKE LOWER(CONCAT('%', :searchText, '%')))
        ORDER BY sp.name, pv.version, ce.title
        """, nativeQuery = true)
    List<CodeExample> findWithFilters(
        @Param("projectSlug") String projectSlug,
        @Param("versionStr") String versionStr,
        @Param("category") String category,
        @Param("searchText") String searchText
    );

    /**
     * Check if example with source URL already exists for a version
     */
    boolean existsByVersionAndSourceUrl(ProjectVersion version, String sourceUrl);

    /**
     * Check if example with title already exists for a version
     * (Used for guides where multiple examples share the same sourceUrl)
     */
    boolean existsByVersionAndTitle(ProjectVersion version, String title);
}
