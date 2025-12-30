package com.spring.mcp.service.mcp;

import com.spring.mcp.model.entity.McpConnection;
import com.spring.mcp.model.entity.McpRequest;
import com.spring.mcp.repository.McpConnectionRepository;
import com.spring.mcp.repository.McpRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Service for logging MCP connections and requests
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class McpRequestLoggerService {

    private final McpConnectionRepository connectionRepository;
    private final McpRequestRepository requestRepository;

    /**
     * Create a new MCP connection log entry
     */
    @Transactional
    public McpConnection createConnection(String clientId) {
        McpConnection connection = McpConnection.builder()
            .clientId(clientId)
            .connectedAt(LocalDateTime.now())
            .requestsCount(0)
            .build();

        McpConnection saved = connectionRepository.save(connection);
        log.info("MCP Connection created: id={}, clientId={}", saved.getId(), clientId);
        return saved;
    }

    /**
     * Log an MCP tool request
     */
    @Transactional
    public void logRequest(Long connectionId, String toolName,
                          Map<String, Object> parameters,
                          String status, int executionTimeMs) {

        connectionRepository.findById(connectionId).ifPresent(connection -> {
            McpRequest request = McpRequest.builder()
                .connection(connection)
                .toolName(toolName)
                .parameters(parameters)
                .responseStatus(status)
                .executionTimeMs(executionTimeMs)
                .build();

            requestRepository.save(request);
            connection.incrementRequestCount();
            connectionRepository.save(connection);

            log.info("MCP Request logged - Connection: {}, Tool: {}, Status: {}, Time: {}ms",
                connectionId, toolName, status, executionTimeMs);
        });
    }

    /**
     * Close an MCP connection
     */
    @Transactional
    public void closeConnection(Long connectionId) {
        connectionRepository.findById(connectionId).ifPresent(connection -> {
            connection.setDisconnectedAt(LocalDateTime.now());
            connectionRepository.save(connection);
            log.info("MCP Connection closed: id={}, clientId={}, requestsCount={}",
                connection.getId(), connection.getClientId(), connection.getRequestsCount());
        });
    }

    /**
     * Get total connections count
     */
    public long getTotalConnections() {
        return connectionRepository.count();
    }

    /**
     * Get total requests count
     */
    public long getTotalRequests() {
        return requestRepository.count();
    }
}
