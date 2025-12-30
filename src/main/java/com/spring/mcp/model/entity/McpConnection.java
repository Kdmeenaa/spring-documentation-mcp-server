package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing MCP client connections for logging and monitoring
 */
@Entity
@Table(name = "mcp_connections")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"requests"})
@EqualsAndHashCode(of = {"id"})
public class McpConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id")
    private String clientId;

    @CreatedDate
    @Column(name = "connected_at", nullable = false, updatable = false)
    private LocalDateTime connectedAt;

    @Column(name = "disconnected_at")
    private LocalDateTime disconnectedAt;

    @Column(name = "requests_count")
    @Builder.Default
    private Integer requestsCount = 0;

    @OneToMany(mappedBy = "connection", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<McpRequest> requests = new ArrayList<>();

    /**
     * Check if connection is still active
     */
    public boolean isActive() {
        return disconnectedAt == null;
    }

    /**
     * Increment request count
     */
    public void incrementRequestCount() {
        this.requestsCount++;
    }
}
