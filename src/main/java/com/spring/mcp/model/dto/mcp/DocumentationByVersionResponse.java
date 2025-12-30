package com.spring.mcp.model.dto.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for getDocumentationByVersion tool
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentationByVersionResponse {
    private String project;
    private String projectSlug;
    private String version;
    private String versionType;
    private Boolean isLatest;
    private Integer totalDocuments;
    private Long executionTimeMs;
    private Map<String, List<DocumentationItem>> documentationByType;
    private List<DocumentationItem> allDocuments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DocumentationItem {
        private Long id;
        private String title;
        private String url;
        private String description;
        private String project;
        private String projectSlug;
        private String version;
        private String docType;
        private String contentType;
        private String snippet;
        private Double rank;
        private String relevance;
    }
}
