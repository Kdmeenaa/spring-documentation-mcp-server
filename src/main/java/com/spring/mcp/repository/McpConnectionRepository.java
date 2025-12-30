package com.spring.mcp.repository;

import com.spring.mcp.model.entity.McpConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for McpConnection entities
 */
@Repository
public interface McpConnectionRepository extends JpaRepository<McpConnection, Long> {

    /**
     * Find connection by client ID
     */
    Optional<McpConnection> findByClientId(String clientId);

    /**
     * Find all active connections (not disconnected)
     */
    List<McpConnection> findByDisconnectedAtIsNull();

    /**
     * Find connections within a time range
     */
    List<McpConnection> findByConnectedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Count active connections
     */
    @Query("SELECT COUNT(c) FROM McpConnection c WHERE c.disconnectedAt IS NULL")
    long countActiveConnections();

    /**
     * Find most recent connections
     */
    List<McpConnection> findTop10ByOrderByConnectedAtDesc();
}
