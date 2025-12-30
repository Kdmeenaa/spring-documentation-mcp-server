package com.spring.mcp.controller.web;

import com.spring.mcp.model.entity.ProjectVersion;
import com.spring.mcp.model.entity.SpringBootVersion;
import com.spring.mcp.model.entity.SpringProject;
import com.spring.mcp.repository.DocumentationLinkRepository;
import com.spring.mcp.repository.ProjectVersionRepository;
import com.spring.mcp.repository.SpringBootVersionRepository;
import com.spring.mcp.repository.SpringProjectRepository;
import com.spring.mcp.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for managing project versions.
 * Handles display and navigation of versions for Spring projects.
 *
 * <p>This controller provides endpoints for:
 * <ul>
 *   <li>Listing all versions across all projects</li>
 *   <li>Viewing versions for a specific project</li>
 *   <li>Displaying version details</li>
 * </ul>
 *
 * <p>Version management supports filtering by Spring Boot version compatibility
 * and provides the same UX as the projects view.
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-08
 */
@Controller
@RequestMapping("/versions")
@RequiredArgsConstructor
@Slf4j
public class VersionsController {

    private final ProjectVersionRepository projectVersionRepository;
    private final SpringProjectRepository springProjectRepository;
    private final SpringBootVersionRepository springBootVersionRepository;
    private final DocumentationLinkRepository documentationLinkRepository;
    private final SettingsService settingsService;

    /**
     * List all versions across all projects.
     *
     * <p>Displays all versions from all projects in a flattened list view,
     * similar to the projects list. Each version is shown as a separate row
     * with columns: Name | Version | Support | Status | Latest | OSS Support End |
     * Enterprise Support End | Reference Docs | API Docs.
     *
     * <p>Optionally filters versions by Spring Boot version compatibility.
     * When a Spring Boot version ID is provided via the {@code springBootVersionId}
     * parameter, only versions compatible with the selected Spring Boot version are shown.
     *
     * @param springBootVersionId optional Spring Boot version ID for filtering
     * @param model Spring MVC model to add attributes for the view
     * @return view name "versions/list" which renders the version list template
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String listVersions(
            @RequestParam(required = false) Long springBootVersionId,
            Model model) {
        log.debug("Listing versions with Spring Boot version filter: {}", springBootVersionId);

        // Fetch all Spring Boot versions for the dropdown
        List<SpringBootVersion> springBootVersions = springBootVersionRepository.findAllOrderByVersionDesc();
        model.addAttribute("springBootVersions", springBootVersions);

        // Fetch versions - filtered if a Spring Boot version is selected
        List<ProjectVersion> versions;
        SpringBootVersion selectedVersion = null;

        if (springBootVersionId != null) {
            selectedVersion = springBootVersionRepository.findById(springBootVersionId).orElse(null);
            if (selectedVersion != null) {
                log.debug("Filtering by Spring Boot version: {}", selectedVersion.getVersion());
                versions = projectVersionRepository.findAllBySpringBootVersionId(springBootVersionId);
                model.addAttribute("selectedSpringBootVersionId", springBootVersionId);
                model.addAttribute("selectedSpringBootVersion", selectedVersion);
            } else {
                log.warn("Spring Boot version with id {} not found, showing all versions", springBootVersionId);
                versions = projectVersionRepository.findAll(Sort.by(Sort.Direction.DESC, "majorVersion", "minorVersion", "patchVersion"));
            }
        } else {
            versions = projectVersionRepository.findAll(Sort.by(Sort.Direction.DESC, "majorVersion", "minorVersion", "patchVersion"));
        }

        // Filter to show only visible versions (status != null)
        final List<ProjectVersion> visibleVersions =
                versions.stream().filter(ProjectVersion::getVisible).toList();

        // Get enterprise subscription setting
        boolean enterpriseSubscriptionEnabled = settingsService.isEnterpriseSubscriptionEnabled();

        model.addAttribute("versions", visibleVersions);
        model.addAttribute("enterpriseSubscriptionEnabled", enterpriseSubscriptionEnabled);
        model.addAttribute("pageTitle", "Project Versions");
        model.addAttribute("activePage", "versions");

        log.info("Retrieved {} visible versions (filtered: {})", visibleVersions.size(), springBootVersionId != null);
        return "versions/list";
    }

    /**
     * List versions for a specific project identified by its slug.
     *
     * <p>Displays all versions associated with a particular Spring project,
     * ordered by version number (descending). This allows users to view
     * the complete version history for a project including:
     * <ul>
     *   <li>Stable releases</li>
     *   <li>Release candidates</li>
     *   <li>Snapshots</li>
     *   <li>Milestones</li>
     * </ul>
     *
     * @param slug the project slug (e.g., "spring-boot", "spring-framework")
     * @param model Spring MVC model to add attributes for the view
     * @param redirectAttributes redirect attributes for flash messages
     * @return view name "versions/project-versions" or redirect if project not found
     */
    @GetMapping("/project/{slug}")
    @PreAuthorize("isAuthenticated()")
    public String listVersionsByProject(
            @PathVariable String slug,
            Model model,
            RedirectAttributes redirectAttributes) {

        log.debug("Listing versions for project: {}", slug);

        return springProjectRepository.findBySlug(slug)
            .map(project -> {
                List<ProjectVersion> versions = projectVersionRepository
                    .findByProjectOrderByVersionDesc(project);

                model.addAttribute("project", project);
                model.addAttribute("versions", versions);
                model.addAttribute("pageTitle", project.getName() + " Versions");

                // Add statistics
                long totalVersions = versions.size();
                long stableVersions = versions.stream()
                    .filter(v -> v.getState().name().equals("GA"))
                    .count();
                long latestCount = versions.stream()
                    .filter(ProjectVersion::getIsLatest)
                    .count();

                model.addAttribute("totalVersions", totalVersions);
                model.addAttribute("stableVersions", stableVersions);
                model.addAttribute("latestCount", latestCount);

                log.info("Retrieved {} versions for project: {} ({})",
                    versions.size(), project.getName(), project.getSlug());

                model.addAttribute("activePage", "versions");
                return "versions/project-versions";
            })
            .orElseGet(() -> {
                log.warn("Project with slug {} not found", slug);
                redirectAttributes.addFlashAttribute("error",
                    "Project not found: " + slug);
                return "redirect:/projects";
            });
    }

    /**
     * Display details for a specific version.
     *
     * <p>Shows the same comprehensive view as project detail page but for a specific version.
     * The view includes project information, version details, and associated documentation.
     * The back button navigates to /versions instead of /projects.
     *
     * @param id the version ID
     * @param model Spring MVC model to add attributes for the view
     * @param redirectAttributes redirect attributes for flash messages
     * @return view name "versions/detail" or redirect if not found
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public String viewVersion(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        log.debug("Viewing version with id: {}", id);

        return projectVersionRepository.findById(id)
            .map(version -> {
                SpringProject project = version.getProject();

                model.addAttribute("version", version);
                model.addAttribute("project", project);
                model.addAttribute("pageTitle", project.getName() + " " + version.getVersion());
                model.addAttribute("activePage", "versions");

                // For the detail view, we show all versions of this project
                List<ProjectVersion> allVersions = projectVersionRepository.findByProjectId(project.getId());
                final List<ProjectVersion> visibleVersions =
                        allVersions.stream().filter(ProjectVersion::getVisible).toList();

                // Get enterprise subscription setting
                boolean enterpriseSubscriptionEnabled = settingsService.isEnterpriseSubscriptionEnabled();

                model.addAttribute("versions", visibleVersions);
                model.addAttribute("enterpriseSubscriptionEnabled", enterpriseSubscriptionEnabled);

                // Add documentation links and statistics
                var documentationLinks = documentationLinkRepository.findByVersionId(version.getId());
                model.addAttribute("documentationLinks", documentationLinks);
                model.addAttribute("documentationCount", documentationLinks.size());

                // Add Spring Boot versions for filter (empty list as we don't have filter in detail view)
                model.addAttribute("springBootVersions", List.of());

                log.info("Displaying version: {} {} (id: {})",
                    project.getName(), version.getVersion(), version.getId());

                return "versions/detail";
            })
            .orElseGet(() -> {
                log.warn("Version with id {} not found", id);
                redirectAttributes.addFlashAttribute("error", "Version not found");
                return "redirect:/versions";
            });
    }

    /**
     * Search versions by version string.
     *
     * <p>Allows searching for versions across all projects by version number
     * or partial version string (e.g., "3.5", "2024.0").
     *
     * @param query the search query
     * @param model Spring MVC model to add attributes for the view
     * @return view name "versions/search-results"
     */
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public String searchVersions(
            @RequestParam(required = false) String query,
            Model model) {

        log.debug("Searching versions with query: {}", query);

        if (query == null || query.trim().isEmpty()) {
            model.addAttribute("versions", List.of());
            model.addAttribute("query", "");
            model.addAttribute("pageTitle", "Search Versions");
            model.addAttribute("activePage", "versions");
            return "versions/search-results";
        }

        try {
            // Simple search - can be enhanced with more sophisticated queries
            List<ProjectVersion> allVersions = projectVersionRepository.findAll();
            List<ProjectVersion> matchingVersions = allVersions.stream()
                .filter(v -> v.getVersion().toLowerCase().contains(query.toLowerCase()))
                .toList();

            model.addAttribute("versions", matchingVersions);
            model.addAttribute("query", query);
            model.addAttribute("resultCount", matchingVersions.size());
            model.addAttribute("pageTitle", "Search Results");

            log.info("Found {} versions matching query: {}", matchingVersions.size(), query);

            model.addAttribute("activePage", "versions");
            return "versions/search-results";
        } catch (Exception e) {
            log.error("Error searching versions", e);
            model.addAttribute("error", "Failed to search versions");
            return "error/general";
        }
    }

    /**
     * Show form for creating a new version.
     *
     * <p>Security: Only users with ADMIN role can access this endpoint.
     *
     * @param model Spring MVC model to add attributes for the view
     * @return view name "versions/form" which renders the version creation form
     */
    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String showCreateForm(Model model) {
        log.debug("Showing create version form");

        model.addAttribute("version", new ProjectVersion());
        model.addAttribute("projects", springProjectRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
        model.addAttribute("pageTitle", "Create New Version");
        model.addAttribute("activePage", "versions");

        return "versions/form";
    }

    /**
     * Process the creation of a new version.
     *
     * <p>Validates the version data and saves it to the database.
     * The combination of project and version must be unique.
     *
     * <p>Security: Only users with ADMIN role can access this endpoint.
     *
     * @param version the version to create
     * @param bindingResult validation result
     * @param projectId the project ID from the form
     * @param redirectAttributes redirect attributes for flash messages
     * @param model Spring MVC model
     * @return redirect to version detail on success, or back to form on error
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String createVersion(
            @Valid @ModelAttribute("version") ProjectVersion version,
            BindingResult bindingResult,
            @RequestParam("project.id") Long projectId,
            RedirectAttributes redirectAttributes,
            Model model) {

        log.debug("Creating new version: {}", version.getVersion());

        // Set the project
        springProjectRepository.findById(projectId).ifPresentOrElse(
            version::setProject,
            () -> bindingResult.rejectValue("project", "invalid", "Invalid project selected")
        );

        if (bindingResult.hasErrors()) {
            log.warn("Validation errors while creating version: {}", bindingResult.getAllErrors());
            model.addAttribute("projects", springProjectRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
            model.addAttribute("pageTitle", "Create New Version");
            model.addAttribute("activePage", "versions");
            return "versions/form";
        }

        // Check for duplicate version for this project
        if (version.getProject() != null) {
            projectVersionRepository.findByProjectAndVersion(version.getProject(), version.getVersion())
                .ifPresent(existing -> {
                    bindingResult.rejectValue("version", "duplicate",
                        "A version with this number already exists for this project");
                });
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("projects", springProjectRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
            model.addAttribute("pageTitle", "Create New Version");
            model.addAttribute("activePage", "versions");
            return "versions/form";
        }

        try {
            ProjectVersion savedVersion = projectVersionRepository.save(version);
            log.info("Successfully created version: {} {} (id: {})",
                savedVersion.getProject().getName(), savedVersion.getVersion(), savedVersion.getId());
            redirectAttributes.addFlashAttribute("success", "Version created successfully");
            return "redirect:/versions/" + savedVersion.getId();
        } catch (Exception e) {
            log.error("Error creating version", e);
            redirectAttributes.addFlashAttribute("error", "Failed to create version");
            return "redirect:/versions";
        }
    }

    /**
     * Show form for editing an existing version.
     *
     * <p>Security: Only users with ADMIN role can access this endpoint.
     *
     * @param id the version ID to edit
     * @param model Spring MVC model to add attributes for the view
     * @param redirectAttributes redirect attributes for flash messages
     * @return view name "versions/form" or redirect if not found
     */
    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        log.debug("Showing edit form for version id: {}", id);

        return projectVersionRepository.findById(id)
            .map(version -> {
                model.addAttribute("version", version);
                model.addAttribute("projects", springProjectRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
                model.addAttribute("pageTitle", "Edit " + version.getVersion());
                model.addAttribute("activePage", "versions");
                return "versions/form";
            })
            .orElseGet(() -> {
                log.warn("Version with id {} not found for editing", id);
                redirectAttributes.addFlashAttribute("error", "Version not found");
                return "redirect:/versions";
            });
    }

    /**
     * Process the update of an existing version.
     *
     * <p>Validates and updates the version information.
     * The combination of project and version must remain unique.
     *
     * <p>Security: Only users with ADMIN role can access this endpoint.
     *
     * @param id the version ID to update
     * @param version the updated version data
     * @param bindingResult validation result
     * @param projectId the project ID from the form
     * @param redirectAttributes redirect attributes for flash messages
     * @param model Spring MVC model
     * @return redirect to version detail on success, or back to form on error
     */
    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateVersion(
            @PathVariable Long id,
            @Valid @ModelAttribute("version") ProjectVersion version,
            BindingResult bindingResult,
            @RequestParam("project.id") Long projectId,
            RedirectAttributes redirectAttributes,
            Model model) {

        log.debug("Updating version with id: {}", id);

        // Set the project
        springProjectRepository.findById(projectId).ifPresentOrElse(
            version::setProject,
            () -> bindingResult.rejectValue("project", "invalid", "Invalid project selected")
        );

        if (bindingResult.hasErrors()) {
            log.warn("Validation errors while updating version: {}", bindingResult.getAllErrors());
            model.addAttribute("projects", springProjectRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
            model.addAttribute("pageTitle", "Edit Version");
            model.addAttribute("activePage", "versions");
            return "versions/form";
        }

        return projectVersionRepository.findById(id)
            .map(existingVersion -> {
                // Check for duplicate version (excluding current version)
                if (version.getProject() != null) {
                    projectVersionRepository.findByProjectAndVersion(version.getProject(), version.getVersion())
                        .ifPresent(foundVersion -> {
                            if (!foundVersion.getId().equals(id)) {
                                bindingResult.rejectValue("version", "duplicate",
                                    "A version with this number already exists for this project");
                            }
                        });
                }

                if (bindingResult.hasErrors()) {
                    model.addAttribute("projects", springProjectRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
                    model.addAttribute("pageTitle", "Edit Version");
                    model.addAttribute("activePage", "versions");
                    return "versions/form";
                }

                // Update fields
                existingVersion.setVersion(version.getVersion());
                existingVersion.setMajorVersion(version.getMajorVersion());
                existingVersion.setMinorVersion(version.getMinorVersion());
                existingVersion.setPatchVersion(version.getPatchVersion());
                existingVersion.setState(version.getState());
                existingVersion.setIsLatest(version.getIsLatest());
                existingVersion.setIsDefault(version.getIsDefault());
                existingVersion.setReleaseDate(version.getReleaseDate());
                existingVersion.setOssSupportEnd(version.getOssSupportEnd());
                existingVersion.setEnterpriseSupportEnd(version.getEnterpriseSupportEnd());

                try {
                    ProjectVersion updatedVersion = projectVersionRepository.save(existingVersion);
                    log.info("Successfully updated version: {} {} (id: {})",
                        updatedVersion.getProject().getName(), updatedVersion.getVersion(), updatedVersion.getId());
                    redirectAttributes.addFlashAttribute("success", "Version updated successfully");
                    return "redirect:/versions/" + updatedVersion.getId();
                } catch (Exception e) {
                    log.error("Error updating version", e);
                    redirectAttributes.addFlashAttribute("error", "Failed to update version");
                    return "redirect:/versions";
                }
            })
            .orElseGet(() -> {
                log.warn("Version with id {} not found for update", id);
                redirectAttributes.addFlashAttribute("error", "Version not found");
                return "redirect:/versions";
            });
    }

    /**
     * Delete a version.
     *
     * <p>Permanently removes a version and all associated data
     * (documentation links, code examples) from the database.
     * This operation cannot be undone.
     *
     * <p>Security: Only users with ADMIN role can access this endpoint.
     *
     * @param id the version ID to delete
     * @param redirectAttributes redirect attributes for flash messages
     * @return redirect to versions list
     */
    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteVersion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.debug("Attempting to delete version with id: {}", id);

        return projectVersionRepository.findById(id)
            .map(version -> {
                try {
                    String versionString = version.getProject().getName() + " " + version.getVersion();
                    projectVersionRepository.delete(version);
                    log.info("Successfully deleted version: {} (id: {})", versionString, id);
                    redirectAttributes.addFlashAttribute("success",
                        "Version '" + versionString + "' deleted successfully");
                } catch (Exception e) {
                    log.error("Error deleting version with id: {}", id, e);
                    redirectAttributes.addFlashAttribute("error",
                        "Failed to delete version. It may have associated data.");
                }
                return "redirect:/versions";
            })
            .orElseGet(() -> {
                log.warn("Version with id {} not found for deletion", id);
                redirectAttributes.addFlashAttribute("error", "Version not found");
                return "redirect:/versions";
            });
    }
}
