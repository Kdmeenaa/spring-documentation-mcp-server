package com.spring.mcp.controller.api;

import com.spring.mcp.service.mcp.McpRequestLoggerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Test controller to verify MCP server status and configuration
 */
@RestController
@RequestMapping("/api/mcp/test")
@Slf4j
@RequiredArgsConstructor
public class McpTestController {

    private final McpRequestLoggerService loggerService;

    /**
     * Get MCP server information
     */
    @GetMapping("/info")
    public Map<String, Object> getMcpInfo() {
        log.info("MCP info requested");

        return Map.of(
            "serverName", "Spring Documentation MCP Server",
            "version", "1.0.0",
            "status", "running",
            "endpoint", "/mcp/spring/sse",
            "protocol", "SSE (Server-Sent Events)",
            "springAiVersion", "1.0.3",
            "autoConfigured", true,
            "statistics", Map.of(
                "totalConnections", loggerService.getTotalConnections(),
                "totalRequests", loggerService.getTotalRequests()
            ),
            "availableTools", Map.of(
                "searchSpringDocs", "Search Spring documentation with filters",
                "getSpringVersions", "Get available versions for a project",
                "listSpringProjects", "List all available Spring projects"
            ),
            "success", true
        );
    }

    /**
     * Get MCP server statistics
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        log.info("MCP statistics requested");

        return Map.of(
            "totalConnections", loggerService.getTotalConnections(),
            "totalRequests", loggerService.getTotalRequests(),
            "success", true
        );
    }
}
