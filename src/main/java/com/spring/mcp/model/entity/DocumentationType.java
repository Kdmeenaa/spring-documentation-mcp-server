package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing types of documentation
 * (Overview, Learn, Support, Samples, etc.)
 */
@Entity
@Table(name = "documentation_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = {"id", "slug"})
public class DocumentationType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;
}
