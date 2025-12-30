package com.spring.mcp.model.dto.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for listSpringBootVersions tool
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpringBootVersionsResponse {
    private SpringBootVersionFilters filters;
    private Boolean enterpriseSubscriptionEnabled;
    private Integer totalFound;
    private Integer returnedResults;
    private Long executionTimeMs;
    private List<SpringBootVersionInfo> versions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SpringBootVersionFilters {
        private String state;
        private Integer limit;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SpringBootVersionInfo {
        private Long id;
        private String version;
        private Integer majorVersion;
        private Integer minorVersion;
        private Integer patchVersion;
        private String state;
        private Boolean isCurrent;
        private String releasedAt;
        private String ossSupportEnd;
        private String enterpriseSupportEnd;
        private String referenceDocUrl;
        private String apiDocUrl;
        private Boolean ossSupportActive;
        private Boolean enterpriseSupportActive;
        private Boolean isEndOfLife;
        private Boolean supportActive;
    }
}
