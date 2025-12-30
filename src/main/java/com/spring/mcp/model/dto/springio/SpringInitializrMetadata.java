package com.spring.mcp.model.dto.springio;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * DTO for Spring Initializr metadata response.
 * Represents the structure of data from https://start.spring.io
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-08
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpringInitializrMetadata {

    @JsonProperty("bootVersion")
    private BootVersionInfo bootVersion;

    @JsonProperty("dependencies")
    private DependenciesInfo dependencies;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BootVersionInfo {
        private String type;

        @JsonProperty("default")
        private String defaultVersion;

        private List<VersionValue> values;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VersionValue {
        private String id;
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DependenciesInfo {
        private String type;
        private List<DependencyGroup> values;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DependencyGroup {
        private String name;
        private List<Dependency> values;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Dependency {
        private String id;
        private String name;
        private String description;
        private String versionRange;

        @JsonProperty("_links")
        private Links links;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Links {
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        private List<Link> reference;
        private Link home;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Link {
        private String href;
        private String title;
    }
}
