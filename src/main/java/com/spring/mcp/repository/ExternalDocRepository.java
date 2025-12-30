package com.spring.mcp.repository;

import com.spring.mcp.model.entity.ExternalDoc;
import com.spring.mcp.model.entity.SpringProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ExternalDoc entities
 */
@Repository
public interface ExternalDocRepository extends JpaRepository<ExternalDoc, Long> {

    /**
     * Find all active external documentation sources
     */
    List<ExternalDoc> findByActiveTrue();

    /**
     * Find external docs by type
     */
    List<ExternalDoc> findByDocType(String docType);

    /**
     * Find external docs by related project
     */
    List<ExternalDoc> findByRelatedProject(SpringProject project);

    /**
     * Find active external docs by type
     */
    List<ExternalDoc> findByDocTypeAndActiveTrue(String docType);
}
