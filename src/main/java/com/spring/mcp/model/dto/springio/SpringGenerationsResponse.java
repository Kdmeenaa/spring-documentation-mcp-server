package com.spring.mcp.model.dto.springio;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * DTO for Spring Generations API response.
 * Represents the structure from https://spring.io/page-data/projects/generations/page-data.json
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-08
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpringGenerationsResponse {

    private Result result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private ResponseData data;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponseData {
        private AllSpringBootGeneration allSpringBootGeneration;
        private SpringBootProject springBootProject;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AllSpringBootGeneration {
        private List<SpringBootGenerationNode> nodes;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SpringBootGenerationNode {
        private String version;
        private GenerationsMapping generationsMapping;
        private Map<String, String> projectLookup;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GenerationsMapping {
        private String support; // "oss", "enterprise", "end-of-life"

        @JsonProperty("projects")
        private Map<String, Object> projects; // Can be List<String> or Map<String, Map<String, List<String>>>
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SpringBootProject {
        private Fields fields;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Fields {
        private Support support;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Support {
        private List<Generation> generations;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Generation {
        private String generation; // Version like "3.5.x"
        private String initialRelease; // "YYYY-MM" format
        private String ossSupportEnd; // "YYYY-MM" format
        private String enterpriseSupportEnd; // "YYYY-MM" format
    }
}
