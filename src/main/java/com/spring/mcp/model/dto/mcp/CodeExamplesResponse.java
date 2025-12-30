package com.spring.mcp.model.dto.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for getCodeExamples tool
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CodeExamplesResponse {
    private CodeExampleFilters filters;
    private Integer totalFound;
    private Integer returnedResults;
    private Long executionTimeMs;
    private List<CodeExampleItem> examples;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CodeExampleFilters {
        private String query;
        private String project;
        private String version;
        private String language;
        private Integer limit;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CodeExampleItem {
        private Long id;
        private String title;
        private String description;
        private String codeSnippet;
        private String language;
        private String category;
        private List<String> tags;
        private String sourceUrl;
        private String project;
        private String projectSlug;
        private String version;
    }
}
