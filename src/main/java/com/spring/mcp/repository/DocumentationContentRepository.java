package com.spring.mcp.repository;

import com.spring.mcp.model.entity.DocumentationContent;
import com.spring.mcp.model.entity.DocumentationLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for DocumentationContent entities
 */
@Repository
public interface DocumentationContentRepository extends JpaRepository<DocumentationContent, Long> {

    /**
     * Find content by documentation link
     */
    Optional<DocumentationContent> findByLink(DocumentationLink link);

    /**
     * Find content by link ID
     */
    Optional<DocumentationContent> findByLinkId(Long linkId);

    /**
     * Full-text search using PostgreSQL's TSVECTOR
     */
    @Query(value = "SELECT dc.* FROM documentation_content dc " +
                   "WHERE dc.indexed_content @@ plainto_tsquery('english', :query) " +
                   "ORDER BY ts_rank(dc.indexed_content, plainto_tsquery('english', :query)) DESC " +
                   "LIMIT :limit",
           nativeQuery = true)
    List<DocumentationContent> fullTextSearch(
        @Param("query") String query,
        @Param("limit") int limit
    );

    /**
     * Search with version filter
     */
    @Query(value = "SELECT dc.* FROM documentation_content dc " +
                   "JOIN documentation_links dl ON dc.link_id = dl.id " +
                   "WHERE dc.indexed_content @@ plainto_tsquery('english', :query) " +
                   "AND dl.version_id = :versionId " +
                   "ORDER BY ts_rank(dc.indexed_content, plainto_tsquery('english', :query)) DESC " +
                   "LIMIT :limit",
           nativeQuery = true)
    List<DocumentationContent> fullTextSearchByVersion(
        @Param("query") String query,
        @Param("versionId") Long versionId,
        @Param("limit") int limit
    );

    /**
     * Advanced full-text search with comprehensive filters
     * Returns content with rank, title, url, project name for building DTOs
     */
    @Query(value = """
        SELECT CAST(dl.id AS bigint)
        FROM documentation_content dc
        JOIN documentation_links dl ON dc.link_id = dl.id
        JOIN project_versions pv ON dl.version_id = pv.id
        JOIN spring_projects sp ON pv.project_id = sp.id
        JOIN documentation_types dt ON dl.doc_type_id = dt.id
        WHERE dc.indexed_content @@ plainto_tsquery('english', :query)
          AND dl.is_active = true
          AND sp.active = true
          AND (:projectSlug IS NULL OR sp.slug = :projectSlug)
          AND (:version IS NULL OR pv.version = :version)
          AND (:docTypeSlug IS NULL OR dt.slug = :docTypeSlug)
        ORDER BY ts_rank_cd(dc.indexed_content, plainto_tsquery('english', :query)) DESC, dc.updated_at DESC
        LIMIT :limit OFFSET :offset
        """,
        nativeQuery = true)
    List<Long> advancedFullTextSearch(
        @Param("query") String query,
        @Param("projectSlug") String projectSlug,
        @Param("version") String version,
        @Param("docTypeSlug") String docTypeSlug,
        @Param("limit") int limit,
        @Param("offset") int offset
    );

    /**
     * Count total results for advanced search (for pagination)
     */
    @Query(value = """
        SELECT COUNT(dc.id)
        FROM documentation_content dc
        JOIN documentation_links dl ON dc.link_id = dl.id
        JOIN project_versions pv ON dl.version_id = pv.id
        JOIN spring_projects sp ON pv.project_id = sp.id
        JOIN documentation_types dt ON dl.doc_type_id = dt.id
        WHERE dc.indexed_content @@ plainto_tsquery('english', :query)
          AND dl.is_active = true
          AND sp.active = true
          AND (:projectSlug IS NULL OR sp.slug = :projectSlug)
          AND (:version IS NULL OR pv.version = :version)
          AND (:docTypeSlug IS NULL OR dt.slug = :docTypeSlug)
        """,
        nativeQuery = true)
    Long countAdvancedSearch(
        @Param("query") String query,
        @Param("projectSlug") String projectSlug,
        @Param("version") String version,
        @Param("docTypeSlug") String docTypeSlug
    );
}
