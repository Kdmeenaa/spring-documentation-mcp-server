package com.spring.mcp.service.indexing;

import com.spring.mcp.model.entity.DocumentationContent;
import com.spring.mcp.model.entity.DocumentationLink;
import com.spring.mcp.model.entity.DocumentationType;
import com.spring.mcp.model.entity.ProjectVersion;
import com.spring.mcp.repository.DocumentationContentRepository;
import com.spring.mcp.repository.DocumentationLinkRepository;
import com.spring.mcp.service.documentation.DocumentationFetchService;
import jakarta.persistence.EntityManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DocumentationIndexer service
 */
@ExtendWith(MockitoExtension.class)
class DocumentationIndexerTest {

    @Mock
    private DocumentationFetchService fetchService;

    @Mock
    private DocumentationContentRepository contentRepository;

    @Mock
    private DocumentationLinkRepository linkRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private DocumentationIndexer indexer;

    private DocumentationLink testLink;
    private DocumentationContent testContent;

    @BeforeEach
    void setUp() {
        // Set configuration properties
        ReflectionTestUtils.setField(indexer, "batchSize", 10);
        ReflectionTestUtils.setField(indexer, "parallelProcessing", false);
        ReflectionTestUtils.setField(indexer, "maxThreads", 2);

        // Create test entities
        ProjectVersion version = ProjectVersion.builder()
            .id(1L)
            .version("3.5.7")
            .build();

        DocumentationType docType = DocumentationType.builder()
            .id(1L)
            .name("Reference")
            .slug("reference")
            .build();

        testLink = DocumentationLink.builder()
            .id(1L)
            .version(version)
            .docType(docType)
            .title("Test Documentation")
            .url("https://docs.spring.io/spring-boot/reference/3.5.7/")
            .isActive(true)
            .build();

        testContent = DocumentationContent.builder()
            .id(1L)
            .link(testLink)
            .contentType("text/html")
            .content("Spring Boot is an awesome framework for building Java applications.")
            .metadata(new HashMap<>())
            .build();

        testLink.setContent(testContent);
    }

    @Test
    void testIndexDocumentation_Success() {
        // Arrange
        when(fetchService.fetchAndProcessDocumentation(any())).thenReturn(testLink);
        when(contentRepository.save(any())).thenReturn(testContent);
        when(linkRepository.save(any())).thenReturn(testLink);

        // Act
        indexer.indexDocumentation(testLink);

        // Assert
        verify(fetchService).fetchAndProcessDocumentation(testLink);
        verify(contentRepository).save(any(DocumentationContent.class));
        verify(linkRepository).save(any(DocumentationLink.class));
    }

    @Test
    void testIndexDocumentation_NullLink_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> indexer.indexDocumentation(null));
    }

    @Test
    void testIndexDocumentation_InactiveLink_Skipped() {
        // Arrange
        testLink.setIsActive(false);

        // Act
        indexer.indexDocumentation(testLink);

        // Assert
        verify(fetchService, never()).fetchAndProcessDocumentation(any());
    }

    @Test
    void testIndexBatch_EmptyList_ReturnsEmptyStats() {
        // Act
        Map<String, Object> stats = indexer.indexBatch(Collections.emptyList());

        // Assert
        assertEquals(0, stats.get("total"));
        assertEquals(0, stats.get("successful"));
        assertEquals(0, stats.get("failed"));
    }

    @Test
    void testIndexBatch_MultipleLinks_Success() {
        // Arrange
        List<DocumentationLink> links = Arrays.asList(testLink, testLink, testLink);
        when(fetchService.fetchAndProcessDocumentation(any())).thenReturn(testLink);
        when(contentRepository.save(any())).thenReturn(testContent);
        when(linkRepository.save(any())).thenReturn(testLink);

        // Act
        Map<String, Object> stats = indexer.indexBatch(links);

        // Assert
        assertEquals(3, stats.get("total"));
        assertEquals(3, stats.get("successful"));
        assertEquals(0, stats.get("failed"));
        verify(fetchService, times(3)).fetchAndProcessDocumentation(any());
    }

    @Test
    void testUpdateIndex_NoExistingContent_PerformsFullIndex() {
        // Arrange
        when(contentRepository.findByLinkId(anyLong())).thenReturn(Optional.empty());
        when(fetchService.fetchAndProcessDocumentation(any())).thenReturn(testLink);
        when(contentRepository.save(any())).thenReturn(testContent);
        when(linkRepository.save(any())).thenReturn(testLink);

        // Act
        indexer.updateIndex(testLink);

        // Assert
        verify(fetchService).fetchAndProcessDocumentation(testLink);
        verify(contentRepository).save(any());
    }

    @Test
    void testUpdateIndex_NullLink_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> indexer.updateIndex(null));
    }

    @Test
    void testGenerateSearchVector_ValidContent_ReturnsCleanedText() {
        // Arrange
        String content = "Spring Boot is an AWESOME framework! It simplifies Java development.";

        // Act
        String searchVector = indexer.generateSearchVector(content);

        // Assert
        assertNotNull(searchVector);
        assertFalse(searchVector.isEmpty());
        assertTrue(searchVector.contains("spring"));
        assertTrue(searchVector.contains("boot"));
        assertTrue(searchVector.contains("framework"));
        assertFalse(searchVector.contains("!")); // Special characters removed
    }

    @Test
    void testGenerateSearchVector_NullContent_ReturnsEmpty() {
        // Act
        String searchVector = indexer.generateSearchVector(null);

        // Assert
        assertEquals("", searchVector);
    }

    @Test
    void testExtractMetadata_ValidDocument_ReturnsMetadata() {
        // Arrange
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Spring Boot Guide</title>
                <meta name="description" content="Learn Spring Boot basics">
                <meta name="keywords" content="spring, boot, java">
            </head>
            <body>
                <h1>Spring Boot Documentation</h1>
                <p>This is a comprehensive guide to Spring Boot framework.</p>
            </body>
            </html>
            """;
        Document doc = Jsoup.parse(html, "https://docs.spring.io");
        when(fetchService.extractMetadata(any())).thenReturn(new HashMap<>());

        // Act
        Map<String, Object> metadata = indexer.extractMetadata(doc);

        // Assert
        assertNotNull(metadata);
        assertTrue(metadata.containsKey("wordCount"));
        assertTrue(metadata.containsKey("readingTime"));
        assertTrue(metadata.containsKey("contentType"));
        assertTrue(metadata.containsKey("language"));
    }

    @Test
    void testExtractMetadata_NullDocument_ReturnsEmpty() {
        // Act
        Map<String, Object> metadata = indexer.extractMetadata(null);

        // Assert
        assertTrue(metadata.isEmpty());
    }

    @Test
    void testIndexBatch_WithFailures_ReturnsPartialSuccess() {
        // Arrange
        List<DocumentationLink> links = Arrays.asList(testLink, testLink);
        when(fetchService.fetchAndProcessDocumentation(any()))
            .thenReturn(testLink)
            .thenThrow(new RuntimeException("Fetch failed"));

        // Act
        Map<String, Object> stats = indexer.indexBatch(links);

        // Assert
        assertEquals(2, stats.get("total"));
        assertEquals(1, stats.get("successful"));
        assertEquals(1, stats.get("failed"));
    }
}
