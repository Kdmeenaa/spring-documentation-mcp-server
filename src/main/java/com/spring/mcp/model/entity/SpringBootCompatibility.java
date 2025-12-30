package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity representing compatibility between a Spring Boot version and other Spring project versions.
 * Maps which versions of Spring projects (e.g., Spring AI 1.0.x, Spring Data 2025.1.x)
 * are compatible with specific Spring Boot versions (e.g., Spring Boot 3.5.x).
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-08
 */
@Entity
@Table(name = "spring_boot_compatibility", uniqueConstraints = {
    @UniqueConstraint(name = "unique_boot_compatibility",
                     columnNames = {"spring_boot_version_id", "compatible_project_version_id"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"springBootVersion", "compatibleProjectVersion"})
@EqualsAndHashCode(of = {"id"})
public class SpringBootCompatibility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The Spring Boot version (e.g., Spring Boot 3.5.x)
     * References the spring_boot_versions table (PRIMARY filter table)
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spring_boot_version_id", nullable = false)
    private SpringBootVersion springBootVersion;

    /**
     * The compatible project version (e.g., Spring AI 1.0.x, Spring Data 2025.1.x)
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compatible_project_version_id", nullable = false)
    private ProjectVersion compatibleProjectVersion;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
