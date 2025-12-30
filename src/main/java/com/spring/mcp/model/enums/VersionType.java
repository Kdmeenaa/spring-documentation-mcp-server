package com.spring.mcp.model.enums;

/**
 * Enumeration of Spring project version types.
 * Used to categorize versions based on their release status.
 */
public enum VersionType {
    /**
     * Stable production release
     */
    STABLE,

    /**
     * Release Candidate - pre-release version for testing
     */
    RC,

    /**
     * Development snapshot version
     */
    SNAPSHOT,

    /**
     * Milestone release - feature complete but not final
     */
    MILESTONE
}
