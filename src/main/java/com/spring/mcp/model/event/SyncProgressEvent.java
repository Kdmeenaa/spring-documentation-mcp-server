package com.spring.mcp.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published during comprehensive sync to track progress.
 * Used by SSE endpoint to stream progress to UI.
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-11-12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncProgressEvent {

    /**
     * Current phase number (0-5)
     */
    private int currentPhase;

    /**
     * Total number of phases
     */
    private int totalPhases;

    /**
     * Phase name/description
     */
    private String phaseDescription;

    /**
     * Current status: "running", "completed", "error"
     */
    private String status;

    /**
     * Progress percentage (0-100)
     */
    private int progressPercent;

    /**
     * Optional detail message
     */
    private String message;

    /**
     * Whether this is the final event (sync completed)
     */
    private boolean completed;

    /**
     * Error message if status is "error"
     */
    private String errorMessage;
}
