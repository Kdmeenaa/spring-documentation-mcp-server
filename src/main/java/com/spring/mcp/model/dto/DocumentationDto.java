package com.spring.mcp.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for documentation data transfer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentationDto {
    private Long id;
    private String title;
    private String url;
    private String description;
    private String projectName;
    private String projectSlug;
    private String version;
    private String docType;
    private String snippet;
    private Double rank; // Search ranking score
    private String contentType; // HTML, MARKDOWN, etc.
}
