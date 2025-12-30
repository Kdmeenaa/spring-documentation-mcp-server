package com.spring.mcp.model.dto.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for listProjectsBySpringBootVersion tool
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectsBySpringBootVersionResponse {
    private SpringBootVersionInfo springBootVersion;
    private Integer totalProjects;
    private Integer totalCompatibleVersions;
    private Long executionTimeMs;
    private List<CompatibleProject> projects;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SpringBootVersionInfo {
        private String version;
        private Integer majorVersion;
        private Integer minorVersion;
        private String state;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CompatibleProject {
        private String slug;
        private String name;
        private String description;
        private String homepage;
        private List<CompatibleVersion> compatibleVersions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CompatibleVersion {
        private String version;
        private String state;
        private Boolean isLatest;
        private String referenceDocUrl;
        private String apiDocUrl;
    }
}
