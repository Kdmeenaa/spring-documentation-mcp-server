package com.spring.mcp.repository;

import com.spring.mcp.model.entity.ProjectRelationship;
import com.spring.mcp.model.entity.SpringProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ProjectRelationship entities
 */
@Repository
public interface ProjectRelationshipRepository extends JpaRepository<ProjectRelationship, Long> {

    /**
     * Find all child projects for a given parent project
     */
    List<ProjectRelationship> findByParentProject(SpringProject parentProject);

    /**
     * Find all parent projects for a given child project
     */
    List<ProjectRelationship> findByChildProject(SpringProject childProject);

    /**
     * Find all child projects for a parent project by parent ID
     */
    @Query("SELECT pr FROM ProjectRelationship pr WHERE pr.parentProject.id = :parentProjectId")
    List<ProjectRelationship> findByParentProjectId(@Param("parentProjectId") Long parentProjectId);

    /**
     * Find all parent projects for a child project by child ID
     */
    @Query("SELECT pr FROM ProjectRelationship pr WHERE pr.childProject.id = :childProjectId")
    List<ProjectRelationship> findByChildProjectId(@Param("childProjectId") Long childProjectId);

    /**
     * Check if a relationship exists between parent and child
     */
    boolean existsByParentProjectAndChildProject(SpringProject parentProject, SpringProject childProject);

    /**
     * Find all parent projects (projects that have children)
     */
    @Query("SELECT DISTINCT pr.parentProject FROM ProjectRelationship pr")
    List<SpringProject> findAllParentProjects();

    /**
     * Find all child projects (projects that have parents)
     */
    @Query("SELECT DISTINCT pr.childProject FROM ProjectRelationship pr")
    List<SpringProject> findAllChildProjects();

    /**
     * Delete all relationships for a project (both as parent and child)
     */
    void deleteByParentProjectOrChildProject(SpringProject parentProject, SpringProject childProject);
}
