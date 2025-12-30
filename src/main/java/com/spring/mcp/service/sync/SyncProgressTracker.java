package com.spring.mcp.service.sync;

import com.spring.mcp.model.event.SyncProgressEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service to track and broadcast sync progress to connected SSE clients.
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-11-12
 */
@Service
@Slf4j
public class SyncProgressTracker {

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, SyncProgressEvent> latestProgress = new ConcurrentHashMap<>();

    /**
     * Register an SSE emitter to receive progress updates.
     *
     * @param emitter the SSE emitter
     */
    public void registerEmitter(SseEmitter emitter) {
        emitters.add(emitter);

        // Clean up on completion or timeout
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        log.debug("Registered new SSE emitter. Total emitters: {}", emitters.size());
    }

    /**
     * Publish a progress update to all connected clients.
     *
     * @param event the progress event
     */
    public void publishProgress(SyncProgressEvent event) {
        latestProgress.put("current", event);

        log.debug("Publishing progress: Phase {}/{} - {} ({}%)",
            event.getCurrentPhase(), event.getTotalPhases(),
            event.getPhaseDescription(), event.getProgressPercent());

        // Send to all connected emitters
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("progress")
                    .data(event));
            } catch (IOException e) {
                log.warn("Failed to send progress to emitter: {}", e.getMessage());
                emitters.remove(emitter);
            }
        });

        // If completed, complete all emitters
        if (event.isCompleted()) {
            completeAll();
        }
    }

    /**
     * Get the latest progress event.
     *
     * @return the latest progress event, or null if none
     */
    public SyncProgressEvent getLatestProgress() {
        return latestProgress.get("current");
    }

    /**
     * Clear the latest progress (reset state).
     */
    public void clearProgress() {
        latestProgress.clear();
    }

    /**
     * Complete all registered emitters.
     */
    private void completeAll() {
        emitters.forEach(emitter -> {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.warn("Error completing emitter: {}", e.getMessage());
            }
        });
        emitters.clear();
    }
}
