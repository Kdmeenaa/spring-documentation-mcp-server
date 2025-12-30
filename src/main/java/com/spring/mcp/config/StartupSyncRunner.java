package com.spring.mcp.config;

import com.spring.mcp.service.sync.ComprehensiveSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Triggers a comprehensive sync on application startup.
 * Only active when the 'startup-sync' profile is enabled.
 *
 * @author Spring MCP Server
 * @since 2025-01-08
 */
@Component
@Profile("startup-sync")
@RequiredArgsConstructor
@Slf4j
public class StartupSyncRunner implements CommandLineRunner {

    private final ComprehensiveSyncService comprehensiveSyncService;

    @Override
    public void run(String... args) {
        log.info("========================================");
        log.info("STARTUP SYNC TRIGGERED");
        log.info("========================================");

        try {
            ComprehensiveSyncService.ComprehensiveSyncResult result = comprehensiveSyncService.syncAll();

            log.info("========================================");
            log.info("STARTUP SYNC COMPLETED");
            log.info("Success: {}", result.isSuccess());
            log.info("Projects Created: {}", result.getTotalProjectsCreated());
            log.info("Versions Created: {}", result.getTotalVersionsCreated());
            log.info("Errors: {}", result.getTotalErrors());
            log.info("========================================");
        } catch (Exception e) {
            log.error("Error during startup sync", e);
        }
    }
}
