package com.spring.mcp.service.version;

import com.spring.mcp.model.entity.ProjectVersion;
import com.spring.mcp.model.entity.SpringProject;
import com.spring.mcp.model.enums.VersionState;
import com.spring.mcp.repository.ProjectVersionRepository;
import com.spring.mcp.repository.SpringProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for detecting and managing Spring project versions.
 *
 * This service is responsible for:
 * <ul>
 *   <li>Detecting available versions from spring.io project pages</li>
 *   <li>Parsing version strings (major.minor.patch, RC, SNAPSHOT, M1, etc.)</li>
 *   <li>Identifying the latest stable version</li>
 *   <li>Implementing n-2 strategy (latest + 2 previous minor versions)</li>
 *   <li>Detecting n+1 versions (RC, milestones, snapshots)</li>
 *   <li>Storing and updating version metadata in the database</li>
 * </ul>
 *
 * @author Spring MCP Server
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VersionDetectionService {

    private final ProjectVersionRepository projectVersionRepository;
    private final SpringProjectRepository springProjectRepository;

    /**
     * Regex pattern for parsing version strings.
     * Captures: major.minor.patch[-suffix]
     * Examples: 3.5.7, 3.4.0-RC2, 4.0.0-SNAPSHOT, 3.5.0-M1
     */
    private static final Pattern VERSION_PATTERN = Pattern.compile(
        "^(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:[-.]?(RC|SNAPSHOT|M|RELEASE)(?:\\d+)?)?$",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Base URL for Spring project pages
     */
    private static final String SPRING_IO_BASE_URL = "https://spring.io/projects/";

    /**
     * Date formatters for parsing release dates from spring.io
     */
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("MMM d, yyyy"),
        DateTimeFormatter.ofPattern("MMMM d, yyyy")
    );

    /**
     * Detects and retrieves all available versions for a Spring project from spring.io.
     *
     * This method scrapes the project page on spring.io to extract version information,
     * parses each version string, and creates ProjectVersion entities for storage.
     *
     * @param projectSlug the project slug (e.g., "spring-boot", "spring-framework")
     * @return list of detected ProjectVersion entities (not yet persisted)
     * @throws IllegalArgumentException if the project slug is invalid
     */
    public List<ProjectVersion> detectAvailableVersions(String projectSlug) {
        log.info("Detecting available versions for project: {}", projectSlug);

        SpringProject project = springProjectRepository.findBySlug(projectSlug)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectSlug));

        try {
            String url = SPRING_IO_BASE_URL + projectSlug;
            log.debug("Fetching project page: {}", url);

            Document doc = Jsoup.connect(url)
                .timeout(30000)
                .userAgent("Spring-MCP-Server/1.0")
                .get();

            List<ProjectVersion> versions = new ArrayList<>();

            // Extract versions from the project page
            // Spring.io typically displays versions in a dropdown or section
            Elements versionElements = doc.select(".version-selector option, .release-version, [data-version]");

            if (versionElements.isEmpty()) {
                // Fallback: try to find version info in text content
                versionElements = doc.select("section:contains(version), div:contains(Current)");
                log.debug("Using fallback selector, found {} elements", versionElements.size());
            }

            Set<String> processedVersions = new HashSet<>();

            for (Element element : versionElements) {
                String versionText = extractVersionText(element);

                if (versionText != null && !processedVersions.contains(versionText)) {
                    processedVersions.add(versionText);

                    try {
                        ProjectVersion version = parseVersion(versionText);
                        if (version != null) {
                            version.setProject(project);

                            // Extract release date if available
                            String dateText = element.attr("data-release-date");
                            if (!dateText.isEmpty()) {
                                version.setReleaseDate(parseDate(dateText));
                            }

                            versions.add(version);
                            log.debug("Detected version: {}", versionText);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse version: {}", versionText, e);
                    }
                }
            }

            log.info("Successfully detected {} versions for project: {}", versions.size(), projectSlug);
            return versions;

        } catch (Exception e) {
            log.error("Failed to detect versions for project: {}", projectSlug, e);
            return Collections.emptyList();
        }
    }

    /**
     * Extracts version text from an HTML element.
     *
     * @param element the HTML element containing version information
     * @return the extracted version string, or null if not found
     */
    private String extractVersionText(Element element) {
        // Try various attributes and text content
        String version = element.attr("value");
        if (version.isEmpty()) {
            version = element.attr("data-version");
        }
        if (version.isEmpty()) {
            version = element.text();
        }

        // Clean up the version string
        version = version.trim()
            .replaceAll("^v", "")  // Remove leading 'v'
            .replaceAll("\\s+.*$", "")  // Remove trailing text
            .replaceAll("^Version\\s+", "")
            .replaceAll("^Current:\\s+", "");

        return version.isEmpty() ? null : version;
    }

    /**
     * Parses a version string and creates a ProjectVersion entity.
     *
     * Supported formats:
     * <ul>
     *   <li>3.5.7 (stable)</li>
     *   <li>3.4.0-RC2 (release candidate)</li>
     *   <li>4.0.0-SNAPSHOT (snapshot)</li>
     *   <li>3.5.0-M1 (milestone)</li>
     *   <li>3.5 (minor version only)</li>
     * </ul>
     *
     * @param versionString the version string to parse
     * @return a ProjectVersion entity, or null if parsing fails
     */
    public ProjectVersion parseVersion(String versionString) {
        if (versionString == null || versionString.isBlank()) {
            return null;
        }

        Matcher matcher = VERSION_PATTERN.matcher(versionString.trim());
        if (!matcher.matches()) {
            log.debug("Version string does not match expected pattern: {}", versionString);
            return null;
        }

        try {
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));
            Integer patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : null;
            String suffix = matcher.group(4);

            VersionState versionState = determineVersionState(suffix);

            return ProjectVersion.builder()
                .version(versionString.trim())
                .majorVersion(major)
                .minorVersion(minor)
                .patchVersion(patch)
                .state(versionState)
                .isLatest(false)
                .isDefault(false)
                .build();

        } catch (NumberFormatException e) {
            log.error("Failed to parse version numbers from: {}", versionString, e);
            return null;
        }
    }

    /**
     * Determines the version state based on the version suffix.
     *
     * @param suffix the version suffix (RC, SNAPSHOT, M, RELEASE, or null)
     * @return the corresponding VersionState
     */
    public VersionState determineVersionState(String suffix) {
        if (suffix == null || suffix.equalsIgnoreCase("RELEASE")) {
            return VersionState.GA;
        }

        return switch (suffix.toUpperCase()) {
            case "RC" -> VersionState.RC;
            case "SNAPSHOT" -> VersionState.SNAPSHOT;
            case "M" -> VersionState.MILESTONE;
            default -> VersionState.GA;
        };
    }

    /**
     * Finds the latest stable version from a list of versions.
     *
     * The latest stable version is the highest version number with GA state.
     *
     * @param versions list of project versions to search
     * @return the latest stable version, or null if none found
     */
    public ProjectVersion findLatestStable(List<ProjectVersion> versions) {
        return versions.stream()
            .filter(v -> v.getState() == VersionState.GA)
            .max(Comparator
                .comparingInt(ProjectVersion::getMajorVersion)
                .thenComparingInt(ProjectVersion::getMinorVersion)
                .thenComparing(v -> Objects.requireNonNullElse(v.getPatchVersion(), 0)))
            .orElse(null);
    }

    /**
     * Gets active versions for a project using the n-2 strategy.
     *
     * Returns:
     * <ul>
     *   <li>Latest stable version</li>
     *   <li>Previous 2 minor versions (n-1 and n-2)</li>
     *   <li>n+1 versions (RC, milestones, snapshots of next major/minor)</li>
     * </ul>
     *
     * @param projectSlug the project slug
     * @return list of active versions following the n-2 strategy
     */
    @Transactional(readOnly = true)
    public List<ProjectVersion> getActiveVersions(String projectSlug) {
        log.info("Getting active versions (n-2 strategy) for project: {}", projectSlug);

        SpringProject project = springProjectRepository.findBySlug(projectSlug)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectSlug));

        List<ProjectVersion> allVersions = projectVersionRepository
            .findByProjectOrderByVersionDesc(project);

        if (allVersions.isEmpty()) {
            log.warn("No versions found for project: {}", projectSlug);
            return Collections.emptyList();
        }

        List<ProjectVersion> activeVersions = new ArrayList<>();

        // Find latest stable version
        ProjectVersion latestStable = findLatestStable(allVersions);
        if (latestStable == null) {
            log.warn("No stable version found for project: {}", projectSlug);
            return Collections.emptyList();
        }

        activeVersions.add(latestStable);
        log.debug("Latest stable: {}", latestStable.getVersion());

        // Get n-1 and n-2 minor versions (previous minor versions)
        List<ProjectVersion> previousMinorVersions = allVersions.stream()
            .filter(v -> v.getState() == VersionState.GA)
            .filter(v -> v.getMajorVersion().equals(latestStable.getMajorVersion()))
            .filter(v -> v.getMinorVersion() < latestStable.getMinorVersion())
            .sorted(Comparator
                .comparingInt(ProjectVersion::getMinorVersion).reversed()
                .thenComparing(v -> Objects.requireNonNullElse(v.getPatchVersion(), 0), Comparator.reverseOrder()))
            .limit(2)
            .toList();

        activeVersions.addAll(previousMinorVersions);
        log.debug("Previous minor versions (n-1, n-2): {}",
            previousMinorVersions.stream().map(ProjectVersion::getVersion).collect(Collectors.joining(", ")));

        // Get n+1 versions (future versions: PRERELEASE, SNAPSHOT)
        List<ProjectVersion> futureVersions = allVersions.stream()
            .filter(v -> v.getState() != VersionState.GA)
            .filter(v -> isNewerThan(v, latestStable))
            .sorted(Comparator
                .comparingInt(ProjectVersion::getMajorVersion).reversed()
                .thenComparingInt(ProjectVersion::getMinorVersion).reversed()
                .thenComparing(v -> Objects.requireNonNullElse(v.getPatchVersion(), 0), Comparator.reverseOrder()))
            .limit(3)  // Limit to top 3 future versions
            .toList();

        activeVersions.addAll(futureVersions);
        log.debug("Future versions (n+1): {}",
            futureVersions.stream().map(ProjectVersion::getVersion).collect(Collectors.joining(", ")));

        log.info("Found {} active versions for project: {}", activeVersions.size(), projectSlug);
        return activeVersions;
    }

    /**
     * Checks if version1 is newer than version2.
     *
     * @param version1 the first version
     * @param version2 the second version
     * @return true if version1 is newer than version2
     */
    private boolean isNewerThan(ProjectVersion version1, ProjectVersion version2) {
        int majorComp = version1.getMajorVersion().compareTo(version2.getMajorVersion());
        if (majorComp != 0) {
            return majorComp > 0;
        }

        int minorComp = version1.getMinorVersion().compareTo(version2.getMinorVersion());
        if (minorComp != 0) {
            return minorComp > 0;
        }

        Integer patch1 = Objects.requireNonNullElse(version1.getPatchVersion(), 0);
        Integer patch2 = Objects.requireNonNullElse(version2.getPatchVersion(), 0);
        return patch1.compareTo(patch2) > 0;
    }

    /**
     * Updates version information for a project by detecting and storing new versions.
     *
     * This method:
     * <ol>
     *   <li>Detects available versions from spring.io</li>
     *   <li>Compares with existing versions in the database</li>
     *   <li>Adds new versions</li>
     *   <li>Updates the latest stable version flag</li>
     * </ol>
     *
     * @param projectSlug the project slug
     * @return number of new versions added
     */
    @Transactional
    public int updateVersions(String projectSlug) {
        log.info("Updating versions for project: {}", projectSlug);

        SpringProject project = springProjectRepository.findBySlug(projectSlug)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectSlug));

        List<ProjectVersion> detectedVersions = detectAvailableVersions(projectSlug);
        List<ProjectVersion> existingVersions = projectVersionRepository.findByProject(project);

        Set<String> existingVersionStrings = existingVersions.stream()
            .map(ProjectVersion::getVersion)
            .collect(Collectors.toSet());

        List<ProjectVersion> newVersions = detectedVersions.stream()
            .filter(v -> !existingVersionStrings.contains(v.getVersion()))
            .toList();

        if (!newVersions.isEmpty()) {
            projectVersionRepository.saveAll(newVersions);
            log.info("Added {} new versions for project: {}", newVersions.size(), projectSlug);
        } else {
            log.info("No new versions found for project: {}", projectSlug);
        }

        // Update latest stable flag
        updateLatestStableFlag(project);

        return newVersions.size();
    }

    /**
     * Updates the isLatest flag for all versions of a project.
     * Only the highest stable version will have isLatest=true.
     *
     * @param project the Spring project
     */
    @Transactional
    public void updateLatestStableFlag(SpringProject project) {
        List<ProjectVersion> allVersions = projectVersionRepository.findByProject(project);
        ProjectVersion latestStable = findLatestStable(allVersions);

        if (latestStable != null) {
            // Clear all isLatest flags
            allVersions.forEach(v -> v.setIsLatest(false));

            // Set the latest stable
            latestStable.setIsLatest(true);

            projectVersionRepository.saveAll(allVersions);
            log.info("Updated latest stable version to: {} for project: {}",
                latestStable.getVersion(), project.getName());
        }
    }

    /**
     * Parses a date string using multiple date formats.
     *
     * @param dateText the date string to parse
     * @return the parsed LocalDate, or null if parsing fails
     */
    private LocalDate parseDate(String dateText) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateText, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next formatter
            }
        }
        log.debug("Could not parse date: {}", dateText);
        return null;
    }

    /**
     * Gets all stable versions for a project.
     *
     * @param projectSlug the project slug
     * @return list of stable versions, sorted by version descending
     */
    @Transactional(readOnly = true)
    public List<ProjectVersion> getStableVersions(String projectSlug) {
        SpringProject project = springProjectRepository.findBySlug(projectSlug)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectSlug));

        return projectVersionRepository.findByProjectAndState(project, VersionState.GA)
            .stream()
            .sorted(Comparator
                .comparingInt(ProjectVersion::getMajorVersion).reversed()
                .thenComparingInt(ProjectVersion::getMinorVersion).reversed()
                .thenComparing(v -> Objects.requireNonNullElse(v.getPatchVersion(), 0), Comparator.reverseOrder()))
            .toList();
    }

    /**
     * Gets all pre-release versions (PRERELEASE, SNAPSHOT) for a project.
     *
     * @param projectSlug the project slug
     * @return list of pre-release versions, sorted by version descending
     */
    @Transactional(readOnly = true)
    public List<ProjectVersion> getPreReleaseVersions(String projectSlug) {
        SpringProject project = springProjectRepository.findBySlug(projectSlug)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectSlug));

        return projectVersionRepository.findByProject(project)
            .stream()
            .filter(v -> v.getState() != VersionState.GA)
            .sorted(Comparator
                .comparingInt(ProjectVersion::getMajorVersion).reversed()
                .thenComparingInt(ProjectVersion::getMinorVersion).reversed()
                .thenComparing(v -> Objects.requireNonNullElse(v.getPatchVersion(), 0), Comparator.reverseOrder()))
            .toList();
    }
}
