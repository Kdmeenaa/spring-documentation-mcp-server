package com.spring.mcp.util;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing semantic version strings.
 * Handles various version formats like "1.5.x", "2.0.0-RC1", "3.1.0-SNAPSHOT", etc.
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-08
 */
@Slf4j
public class VersionParser {

    // Pattern to match version formats: major.minor.patch[-suffix]
    // Examples: 1.5.22, 2.0.x, 3.1.0-RC1, 3.2.0-SNAPSHOT
    private static final Pattern VERSION_PATTERN = Pattern.compile(
        "^(\\d+)\\.(\\d+)(?:\\.(\\d+|x|X))?(?:[-.].*)?$"
    );

    /**
     * Parsed version components.
     */
    public static class ParsedVersion {
        private final int majorVersion;
        private final int minorVersion;
        private final int patchVersion;

        public ParsedVersion(int majorVersion, int minorVersion, int patchVersion) {
            this.majorVersion = majorVersion;
            this.minorVersion = minorVersion;
            this.patchVersion = patchVersion;
        }

        public int getMajorVersion() {
            return majorVersion;
        }

        public int getMinorVersion() {
            return minorVersion;
        }

        public int getPatchVersion() {
            return patchVersion;
        }

        @Override
        public String toString() {
            return String.format("%d.%d.%d", majorVersion, minorVersion, patchVersion);
        }
    }

    /**
     * Parse a version string into major, minor, and patch components.
     * Rules:
     * - "1.5.x" -> major=1, minor=5, patch=0
     * - "2.0.0-RC1" -> major=2, minor=0, patch=0
     * - "3.1.22" -> major=3, minor=1, patch=22
     * - "3.1" -> major=3, minor=1, patch=0
     * - If patch version is not a number (x, X, or missing), it defaults to 0
     *
     * @param version the version string to parse
     * @return ParsedVersion object with major, minor, and patch components
     * @throws IllegalArgumentException if the version string cannot be parsed
     */
    public static ParsedVersion parse(String version) {
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("Version string cannot be null or empty");
        }

        String trimmedVersion = version.trim();
        Matcher matcher = VERSION_PATTERN.matcher(trimmedVersion);

        if (!matcher.matches()) {
            log.warn("Unable to parse version: {}. Using defaults 0.0.0", version);
            return new ParsedVersion(0, 0, 0);
        }

        try {
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));
            int patch = 0;

            // Group 3 is the patch version (optional)
            String patchGroup = matcher.group(3);
            if (patchGroup != null && !patchGroup.equalsIgnoreCase("x")) {
                try {
                    patch = Integer.parseInt(patchGroup);
                } catch (NumberFormatException e) {
                    // If patch is not a valid number, default to 0
                    log.debug("Patch version '{}' is not a number, defaulting to 0", patchGroup);
                    patch = 0;
                }
            }

            return new ParsedVersion(major, minor, patch);

        } catch (NumberFormatException e) {
            log.error("Error parsing version components from: {}", version, e);
            return new ParsedVersion(0, 0, 0);
        }
    }

    /**
     * Check if a version string is valid and can be parsed.
     *
     * @param version the version string to validate
     * @return true if the version can be parsed, false otherwise
     */
    public static boolean isValid(String version) {
        if (version == null || version.trim().isEmpty()) {
            return false;
        }
        return VERSION_PATTERN.matcher(version.trim()).matches();
    }
}
