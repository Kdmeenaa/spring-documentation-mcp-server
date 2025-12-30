package com.spring.mcp.model.enums;

/**
 * Enumeration of Spring project version states.
 * Represents the release state/maturity of a version.
 *
 * The states follow the typical Spring release lifecycle:
 * SNAPSHOT → MILESTONE → RC → GA
 */
public enum VersionState {
    /**
     * Development snapshot version - unstable, actively developed
     */
    SNAPSHOT,

    /**
     * Milestone release - feature complete for specific milestone but not final
     */
    MILESTONE,

    /**
     * Release Candidate - pre-release version for final testing
     */
    RC,

    /**
     * General Availability - stable production release
     */
    GA
}
