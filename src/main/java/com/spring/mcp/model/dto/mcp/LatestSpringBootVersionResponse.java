package com.spring.mcp.model.dto.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for getLatestSpringBootVersion tool
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LatestSpringBootVersionResponse {
    private Integer majorVersion;
    private Integer minorVersion;
    private Integer totalVersionsForMajorMinor;
    private Long executionTimeMs;
    private SpringBootVersionsResponse.SpringBootVersionInfo latestVersion;
    private List<SpringBootVersionsResponse.SpringBootVersionInfo> allVersions;
}
