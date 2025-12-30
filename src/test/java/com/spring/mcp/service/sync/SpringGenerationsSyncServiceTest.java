package com.spring.mcp.service.sync;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SpringGenerationsSyncServiceTest {

    @Autowired
    private SpringGenerationsSyncService syncService;

    @Test
    void testSyncAllGenerations() {
        SpringGenerationsSyncService.SyncResult result = syncService.syncAllGenerations();
        System.out.println("Sync completed. Success: " + result.isSuccess());
        System.out.println("Projects created: " + result.getProjectsCreated());
        System.out.println("Versions created: " + result.getVersionsCreated());
        System.out.println("Errors: " + result.getErrorsEncountered());
    }
}
