package com.spring.mcp.repository;

import com.spring.mcp.model.entity.SchedulerSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for SchedulerSettings entity.
 * Since there should only be one settings record, provides convenience method.
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-12
 */
@Repository
public interface SchedulerSettingsRepository extends JpaRepository<SchedulerSettings, Long> {

    /**
     * Find the first (and should be only) scheduler settings record
     *
     * @return Optional containing the settings, or empty if none exists
     */
    Optional<SchedulerSettings> findFirstByOrderByIdAsc();
}
