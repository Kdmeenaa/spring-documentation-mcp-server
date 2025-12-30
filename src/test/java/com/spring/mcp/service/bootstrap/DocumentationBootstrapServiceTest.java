package com.spring.mcp.service.bootstrap;

import com.spring.mcp.model.entity.DocumentationType;
import com.spring.mcp.model.entity.ProjectVersion;
import com.spring.mcp.model.entity.SpringProject;
import com.spring.mcp.model.enums.VersionState;
import com.spring.mcp.repository.DocumentationLinkRepository;
import com.spring.mcp.repository.DocumentationTypeRepository;
import com.spring.mcp.repository.ProjectVersionRepository;
import com.spring.mcp.repository.SpringProjectRepository;
import com.spring.mcp.service.documentation.DocumentationFetchService;
import com.spring.mcp.service.indexing.DocumentationIndexer;
import com.spring.mcp.service.version.VersionDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DocumentationBootstrapService.
 *
 * These tests verify the bootstrap service's ability to:
 * - Create Spring projects and their versions
 * - Generate documentation links for different Spring projects
 * - Track bootstrap status and progress
 * - Handle errors gracefully
 */
@ExtendWith(MockitoExtension.class)
class DocumentationBootstrapServiceTest {

    @Mock
    private SpringProjectRepository springProjectRepository;

    @Mock
    private ProjectVersionRepository projectVersionRepository;

    @Mock
    private DocumentationTypeRepository documentationTypeRepository;

    @Mock
    private DocumentationLinkRepository documentationLinkRepository;

    @Mock
    private VersionDetectionService versionDetectionService;

    @Mock
    private DocumentationFetchService documentationFetchService;

    @Mock
    private DocumentationIndexer documentationIndexer;

    private DocumentationBootstrapService bootstrapService;

    @BeforeEach
    void setUp() {
        bootstrapService = new DocumentationBootstrapService(
            springProjectRepository,
            projectVersionRepository,
            documentationTypeRepository,
            documentationLinkRepository,
            versionDetectionService,
            documentationFetchService,
            documentationIndexer
        );

        // Set configuration properties
        ReflectionTestUtils.setField(bootstrapService, "bootstrapEnabled", true);
        ReflectionTestUtils.setField(bootstrapService, "bootstrapOnStartup", false);
        ReflectionTestUtils.setField(bootstrapService, "bootstrapProjects", List.of("spring-boot", "spring-framework"));
        ReflectionTestUtils.setField(bootstrapService, "springDocsBaseUrl", "https://docs.spring.io");
    }

    @Test
    void testGetBootstrapStatus_Initial() {
        // When
        Map<String, Object> status = bootstrapService.getBootstrapStatus();

        // Then
        assertThat(status).isNotNull();
        assertThat(status.get("enabled")).isEqualTo(true);
        assertThat(status.get("inProgress")).isEqualTo(false);
        assertThat(status.get("completed")).isEqualTo(false);
        assertThat(status).containsKey("statistics");

        @SuppressWarnings("unchecked")
        Map<String, Object> statistics = (Map<String, Object>) status.get("statistics");
        assertThat(statistics.get("totalProjects")).isEqualTo(0);
        assertThat(statistics.get("totalVersions")).isEqualTo(0);
        assertThat(statistics.get("totalLinks")).isEqualTo(0);
    }

    @Test
    void testIsBootstrapComplete_InitiallyFalse() {
        // Then
        assertThat(bootstrapService.isBootstrapComplete()).isFalse();
    }

    @Test
    void testCreateDocumentationLinksForVersion_NullVersion() {
        // When/Then
        assertThatThrownBy(() -> bootstrapService.createDocumentationLinksForVersion(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("ProjectVersion cannot be null");
    }

    @Test
    void testCreateDocumentationLinksForVersion_SpringBoot() {
        // Given
        SpringProject project = SpringProject.builder()
            .id(1L)
            .name("Spring Boot")
            .slug("spring-boot")
            .build();

        ProjectVersion version = ProjectVersion.builder()
            .id(1L)
            .project(project)
            .version("3.5.7")
            .majorVersion(3)
            .minorVersion(5)
            .patchVersion(7)
            .state(VersionState.GA)
            .build();

        DocumentationType referenceType = DocumentationType.builder()
            .id(1L)
            .name("Reference")
            .slug("reference")
            .build();

        DocumentationType apiType = DocumentationType.builder()
            .id(2L)
            .name("API Documentation")
            .slug("api")
            .build();

        DocumentationType learnType = DocumentationType.builder()
            .id(3L)
            .name("Learn")
            .slug("learn")
            .build();

        when(documentationTypeRepository.findBySlug("reference")).thenReturn(Optional.of(referenceType));
        when(documentationTypeRepository.findBySlug("api")).thenReturn(Optional.of(apiType));
        when(documentationTypeRepository.findBySlug("learn")).thenReturn(Optional.of(learnType));
        when(documentationLinkRepository.findByVersionId(1L)).thenReturn(List.of());

        // When
        int linksCreated = bootstrapService.createDocumentationLinksForVersion(version);

        // Then
        assertThat(linksCreated).isEqualTo(2); // Reference + API
        verify(documentationLinkRepository).saveAll(argThat(links -> {
            assertThat(links).hasSize(2);
            List<com.spring.mcp.model.entity.DocumentationLink> linkList =
                (List<com.spring.mcp.model.entity.DocumentationLink>) links;
            assertThat(linkList.get(0).getTitle()).contains("Spring Boot");
            assertThat(linkList.get(0).getUrl()).contains("spring-boot/reference/3.5.7");
            assertThat(linkList.get(1).getUrl()).contains("spring-boot/api/java");
            return true;
        }));
    }

    @Test
    void testCreateDocumentationLinksForVersion_AlreadyExists() {
        // Given
        SpringProject project = SpringProject.builder()
            .id(1L)
            .slug("spring-boot")
            .name("Spring Boot")
            .build();

        ProjectVersion version = ProjectVersion.builder()
            .id(1L)
            .project(project)
            .version("3.5.7")
            .build();

        DocumentationType referenceType = DocumentationType.builder()
            .id(1L)
            .name("Reference")
            .slug("reference")
            .build();

        DocumentationType apiType = DocumentationType.builder()
            .id(2L)
            .name("API Documentation")
            .slug("api")
            .build();

        DocumentationType learnType = DocumentationType.builder()
            .id(3L)
            .name("Learn")
            .slug("learn")
            .build();

        when(documentationTypeRepository.findBySlug("reference")).thenReturn(Optional.of(referenceType));
        when(documentationTypeRepository.findBySlug("api")).thenReturn(Optional.of(apiType));
        when(documentationTypeRepository.findBySlug("learn")).thenReturn(Optional.of(learnType));
        when(documentationLinkRepository.findByVersionId(1L))
            .thenReturn(List.of(new com.spring.mcp.model.entity.DocumentationLink()));

        // When
        int linksCreated = bootstrapService.createDocumentationLinksForVersion(version);

        // Then
        assertThat(linksCreated).isEqualTo(0);
        verify(documentationLinkRepository, never()).saveAll(any());
    }

    @Test
    void testBootstrapProject_UnknownProject() {
        // When/Then
        assertThatThrownBy(() -> bootstrapService.bootstrapProject("unknown-project"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Unknown project: unknown-project");
    }

    @Test
    void testCreateDocumentationTypes() {
        // Given
        when(documentationTypeRepository.findBySlug(anyString())).thenReturn(Optional.empty());
        when(documentationTypeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ReflectionTestUtils.invokeMethod(bootstrapService, "createDocumentationTypes");

        // Then - verify all doc types were created
        verify(documentationTypeRepository, times(6)).save(any(DocumentationType.class));
    }

    @Test
    void testCreateOrGetProject_NewProject() {
        // Given
        when(springProjectRepository.findBySlug("spring-boot")).thenReturn(Optional.empty());
        when(springProjectRepository.save(any())).thenAnswer(invocation -> {
            SpringProject project = invocation.getArgument(0);
            project.setId(1L);
            return project;
        });

        // When
        DocumentationBootstrapService.ProjectDefinition definition =
            new DocumentationBootstrapService.ProjectDefinition(
                "Spring Boot",
                "Description",
                "https://spring.io/projects/spring-boot",
                "https://github.com/spring-projects/spring-boot",
                List.of("3.5.7"),
                List.of()
            );

        SpringProject result = ReflectionTestUtils.invokeMethod(
            bootstrapService, "createOrGetProject", "spring-boot", definition);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Spring Boot");
        assertThat(result.getSlug()).isEqualTo("spring-boot");
        verify(springProjectRepository).save(any(SpringProject.class));
    }

    @Test
    void testCreateOrGetProject_ExistingProject() {
        // Given
        SpringProject existingProject = SpringProject.builder()
            .id(1L)
            .name("Spring Boot")
            .slug("spring-boot")
            .build();

        when(springProjectRepository.findBySlug("spring-boot")).thenReturn(Optional.of(existingProject));

        // When
        DocumentationBootstrapService.ProjectDefinition definition =
            new DocumentationBootstrapService.ProjectDefinition(
                "Spring Boot",
                "Description",
                "https://spring.io/projects/spring-boot",
                "https://github.com/spring-projects/spring-boot",
                List.of("3.5.7"),
                List.of()
            );

        SpringProject result = ReflectionTestUtils.invokeMethod(
            bootstrapService, "createOrGetProject", "spring-boot", definition);

        // Then
        assertThat(result).isEqualTo(existingProject);
        verify(springProjectRepository, never()).save(any());
    }

    @Test
    void testCreateVersionIfNotExists_NewVersion() {
        // Given
        SpringProject project = SpringProject.builder()
            .id(1L)
            .slug("spring-boot")
            .build();

        ProjectVersion parsedVersion = ProjectVersion.builder()
            .version("3.5.7")
            .majorVersion(3)
            .minorVersion(5)
            .patchVersion(7)
            .build();

        when(projectVersionRepository.findByProjectAndVersion(project, "3.5.7"))
            .thenReturn(Optional.empty());
        when(versionDetectionService.parseVersion("3.5.7")).thenReturn(parsedVersion);
        when(projectVersionRepository.save(any())).thenAnswer(invocation -> {
            ProjectVersion v = invocation.getArgument(0);
            v.setId(1L);
            return v;
        });

        // When
        ProjectVersion result = ReflectionTestUtils.invokeMethod(
            bootstrapService, "createVersionIfNotExists", project, "3.5.7", VersionState.GA);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getVersion()).isEqualTo("3.5.7");
        assertThat(result.getProject()).isEqualTo(project);
        assertThat(result.getState()).isEqualTo(VersionState.GA);
        verify(projectVersionRepository).save(any(ProjectVersion.class));
    }

    @Test
    void testCreateVersionIfNotExists_ExistingVersion() {
        // Given
        SpringProject project = SpringProject.builder()
            .id(1L)
            .slug("spring-boot")
            .build();

        ProjectVersion existingVersion = ProjectVersion.builder()
            .id(1L)
            .version("3.5.7")
            .project(project)
            .build();

        when(projectVersionRepository.findByProjectAndVersion(project, "3.5.7"))
            .thenReturn(Optional.of(existingVersion));

        // When
        ProjectVersion result = ReflectionTestUtils.invokeMethod(
            bootstrapService, "createVersionIfNotExists", project, "3.5.7", VersionState.GA);

        // Then
        assertThat(result).isEqualTo(existingVersion);
        verify(versionDetectionService, never()).parseVersion(any());
        verify(projectVersionRepository, never()).save(any());
    }
}
