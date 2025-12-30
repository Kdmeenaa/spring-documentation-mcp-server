package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity representing a parent-child relationship between Spring projects.
 * Examples: Spring Data (parent) → Spring Data JPA, Spring Data MongoDB (children)
 */
@Entity
@Table(name = "project_relationships", uniqueConstraints = {
    @UniqueConstraint(name = "unique_parent_child", columnNames = {"parent_project_id", "child_project_id"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"parentProject", "childProject"})
@EqualsAndHashCode(of = {"id"})
public class ProjectRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_project_id", nullable = false)
    private SpringProject parentProject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_project_id", nullable = false)
    private SpringProject childProject;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
