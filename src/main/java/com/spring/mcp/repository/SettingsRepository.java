package com.spring.mcp.repository;

import com.spring.mcp.model.entity.Settings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Settings entity.
 * This repository handles the singleton Settings entity.
 */
@Repository
public interface SettingsRepository extends JpaRepository<Settings, Long> {

    /**
     * Find the first (and only) settings record.
     * Since settings uses a singleton pattern, there should only be one row.
     */
    Optional<Settings> findFirstBy();
}
