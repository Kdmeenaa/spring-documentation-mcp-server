package com.spring.mcp.model.dto.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for getSpringVersions tool
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VersionsResponse {
    private String project;
    private String slug;
    private String description;
    private List<VersionInfo> versions;
    private Integer count;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class VersionInfo {
        private String version;
        private String type;
        private Boolean isLatest;
        private Boolean isDefault;
        private String releaseDate;
    }
}
