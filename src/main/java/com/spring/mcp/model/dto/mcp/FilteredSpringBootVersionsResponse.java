package com.spring.mcp.model.dto.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for filterSpringBootVersionsBySupport tool
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FilteredSpringBootVersionsResponse {
    private SupportFilters filters;
    private Boolean enterpriseSubscriptionEnabled;
    private String supportDateUsed;
    private Integer totalFound;
    private Long executionTimeMs;
    private List<SpringBootVersionsResponse.SpringBootVersionInfo> versions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SupportFilters {
        private String supportActive;
        private Integer limit;
    }
}
