package com.spring.mcp.service.indexing;

import com.spring.mcp.model.entity.CodeExample;
import com.spring.mcp.model.entity.DocumentationLink;
import com.spring.mcp.repository.CodeExampleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service responsible for extracting code examples from documentation HTML content.
 * <p>
 * This service provides comprehensive code example extraction capabilities including:
 * <ul>
 *   <li>Detection of various code block formats (pre/code, div.code, div.highlight, etc.)</li>
 *   <li>Automatic programming language detection from class attributes and content patterns</li>
 *   <li>Context and description extraction from surrounding elements</li>
 *   <li>Intelligent tag generation based on code content and context</li>
 *   <li>Support for multiple syntax highlighter formats (hljs, highlight, prism, github)</li>
 *   <li>Detection of common languages: Java, Kotlin, XML, YAML, SQL, Bash, Properties, JSON, etc.</li>
 * </ul>
 * <p>
 * The extractor handles common documentation patterns found in Spring documentation,
 * including AsciiDoc-generated HTML, Markdown-generated HTML, and custom documentation formats.
 *
 * @author Spring MCP Server
 * @version 1.0.0
 * @since Phase 3
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CodeExampleExtractor {

    private final CodeExampleRepository codeExampleRepository;

    // CSS selectors for code block detection
    private static final String[] CODE_BLOCK_SELECTORS = {
        "pre > code",                    // Standard HTML5 code blocks
        "pre.highlight code",            // AsciiDoc/Rouge style
        "div.highlight pre",             // GitHub/Jekyll style
        "div.code pre",                  // Custom documentation style
        "div.listingblock pre",          // AsciiDoc listing blocks
        "div.source-code pre",           // Spring docs style
        "pre[class*='language-']",       // Prism.js style
        "code[class*='language-']",      // Prism.js inline
        "pre[class*='hljs']",            // Highlight.js style
        "div.example pre",               // Example blocks
        "figure.highlight pre"           // Jekyll/Rouge style
    };

    // Language detection patterns for class attributes
    private static final Pattern LANGUAGE_CLASS_PATTERN = Pattern.compile(
        "(?:language-|lang-|hljs-|brush:|highlight-)?([a-z0-9]+)",
        Pattern.CASE_INSENSITIVE
    );

    // Code content patterns for language detection
    private static final Map<String, Pattern> LANGUAGE_PATTERNS = Map.ofEntries(
        Map.entry("java", Pattern.compile("\\b(?:package|import|public\\s+class|private\\s+class|@Override|@SpringBootApplication)\\b")),
        Map.entry("kotlin", Pattern.compile("\\b(?:fun\\s+|val\\s+|var\\s+|data\\s+class|sealed\\s+class)\\b")),
        Map.entry("xml", Pattern.compile("^\\s*<\\?xml|<beans|<project|<dependencies|<configuration")),
        Map.entry("yaml", Pattern.compile("^[a-zA-Z0-9_-]+:\\s*$", Pattern.MULTILINE)),
        Map.entry("properties", Pattern.compile("^[a-zA-Z0-9._-]+=.+$", Pattern.MULTILINE)),
        Map.entry("sql", Pattern.compile("\\b(?:SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|FROM|WHERE|JOIN)\\b", Pattern.CASE_INSENSITIVE)),
        Map.entry("bash", Pattern.compile("^\\$\\s+|#!/bin/(?:bash|sh)")),
        Map.entry("shell", Pattern.compile("^\\$\\s+|#!/bin/(?:bash|sh)")),
        Map.entry("json", Pattern.compile("^\\s*[{\\[]|\"[^\"]+\"\\s*:")),
        Map.entry("groovy", Pattern.compile("\\b(?:def\\s+|@Grab|@GrabResolver|class\\s+\\w+\\s*\\{)\\b")),
        Map.entry("gradle", Pattern.compile("\\b(?:plugins|dependencies|implementation|testImplementation|repositories)\\s*\\{")),
        Map.entry("maven", Pattern.compile("<project|<modelVersion|<artifactId|<dependencies")),
        Map.entry("dockerfile", Pattern.compile("^\\s*(?:FROM|RUN|COPY|WORKDIR|EXPOSE|CMD|ENTRYPOINT)\\b", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE)),
        Map.entry("html", Pattern.compile("<!DOCTYPE html>|<html|<head|<body|<div|<span", Pattern.CASE_INSENSITIVE)),
        Map.entry("javascript", Pattern.compile("\\b(?:function|const|let|var|=>|console\\.log)\\b")),
        Map.entry("typescript", Pattern.compile("\\b(?:interface|type|enum|implements|export)\\b")),
        Map.entry("css", Pattern.compile("[.#][a-zA-Z][a-zA-Z0-9-_]*\\s*\\{|@media|@import"))
    );

    // Common programming keywords for tag generation
    private static final Map<String, List<String>> KEYWORD_TAGS = Map.ofEntries(
        Map.entry("@SpringBootApplication", List.of("spring-boot", "application", "configuration")),
        Map.entry("@RestController", List.of("rest", "controller", "web")),
        Map.entry("@Service", List.of("service", "business-logic")),
        Map.entry("@Repository", List.of("repository", "data-access")),
        Map.entry("@Entity", List.of("jpa", "entity", "database")),
        Map.entry("@Configuration", List.of("configuration", "bean")),
        Map.entry("@Autowired", List.of("dependency-injection", "autowiring")),
        Map.entry("@Component", List.of("component", "bean")),
        Map.entry("@Bean", List.of("bean", "configuration")),
        Map.entry("@RequestMapping", List.of("web", "mapping", "endpoint")),
        Map.entry("@GetMapping", List.of("web", "get", "endpoint")),
        Map.entry("@PostMapping", List.of("web", "post", "endpoint")),
        Map.entry("@Transactional", List.of("transaction", "database")),
        Map.entry("@Test", List.of("testing", "unit-test")),
        Map.entry("SELECT", List.of("sql", "query", "database")),
        Map.entry("INSERT", List.of("sql", "insert", "database")),
        Map.entry("docker", List.of("docker", "container", "deployment")),
        Map.entry("kubernetes", List.of("kubernetes", "k8s", "deployment"))
    );

    // Maximum code snippet length to store
    private static final int MAX_CODE_LENGTH = 50000;

    // Maximum description length
    private static final int MAX_DESCRIPTION_LENGTH = 2000;

    /**
     * Extracts all code examples from a parsed HTML document and associates them
     * with the given documentation link.
     * <p>
     * This method scans the document for various code block formats, extracts the code,
     * detects the programming language, generates contextual information, and saves
     * the examples to the database.
     *
     * @param doc  the parsed JSoup Document containing the HTML content
     * @param link the DocumentationLink entity to associate the examples with
     * @return a list of extracted and saved CodeExample entities
     * @throws IllegalArgumentException if doc or link is null
     */
    @Transactional
    public List<CodeExample> extractCodeExamples(Document doc, DocumentationLink link) {
        if (doc == null) {
            throw new IllegalArgumentException("Document cannot be null");
        }
        if (link == null) {
            throw new IllegalArgumentException("DocumentationLink cannot be null");
        }

        log.debug("Extracting code examples from documentation: {}", link.getUrl());

        List<CodeExample> examples = new ArrayList<>();
        Set<String> processedCodeHashes = new HashSet<>();

        // Try each selector to find code blocks
        for (String selector : CODE_BLOCK_SELECTORS) {
            Elements codeElements = doc.select(selector);
            log.debug("Found {} code blocks using selector: {}", codeElements.size(), selector);

            for (Element codeElement : codeElements) {
                try {
                    CodeExample example = extractSingleCodeExample(codeElement, link);

                    if (example != null) {
                        // Avoid duplicates using code hash
                        String codeHash = generateCodeHash(example.getCodeSnippet());

                        if (!processedCodeHashes.contains(codeHash)) {
                            processedCodeHashes.add(codeHash);
                            examples.add(example);
                            log.debug("Extracted code example: {} ({})", example.getTitle(), example.getLanguage());
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to extract code example from element: {}", e.getMessage());
                }
            }
        }

        // Save all extracted examples
        if (!examples.isEmpty()) {
            List<CodeExample> savedExamples = codeExampleRepository.saveAll(examples);
            log.info("Saved {} code examples for documentation: {}", savedExamples.size(), link.getUrl());
            return savedExamples;
        }

        log.debug("No code examples found in documentation: {}", link.getUrl());
        return Collections.emptyList();
    }

    /**
     * Extracts a single code example from a code block element.
     *
     * @param codeElement the JSoup Element containing the code
     * @param link        the DocumentationLink to associate with
     * @return a CodeExample entity or null if extraction fails
     */
    private CodeExample extractSingleCodeExample(Element codeElement, DocumentationLink link) {
        String code = extractCode(codeElement);

        if (code == null || code.isBlank() || code.length() < 10) {
            return null; // Skip empty or trivial code blocks
        }

        // Truncate overly long code snippets
        if (code.length() > MAX_CODE_LENGTH) {
            log.warn("Code snippet too long ({}), truncating to {}", code.length(), MAX_CODE_LENGTH);
            code = code.substring(0, MAX_CODE_LENGTH) + "\n... (truncated)";
        }

        String language = detectLanguage(codeElement, code);
        String context = extractContext(codeElement);
        String title = generateTitle(codeElement, code, language);
        List<String> tags = generateTags(code, language, context);

        return CodeExample.builder()
            .version(link.getVersion())
            .title(title)
            .description(context)
            .codeSnippet(code)
            .language(language)
            .category(determineCategoryFromContext(context, tags))
            .tags(tags.toArray(new String[0]))
            .sourceUrl(link.getUrl())
            .build();
    }

    /**
     * Extracts the actual code text from a code element.
     * Handles both pre > code and standalone pre/code elements.
     *
     * @param codeElement the element containing code
     * @return the extracted code as a String
     */
    private String extractCode(Element codeElement) {
        if (codeElement == null) {
            return null;
        }

        // Get text while preserving whitespace
        String code = codeElement.wholeText();

        // If empty, try getting from nested code element
        if (code.isBlank() && "pre".equalsIgnoreCase(codeElement.tagName())) {
            Element codeChild = codeElement.selectFirst("code");
            if (codeChild != null) {
                code = codeChild.wholeText();
            }
        }

        return code != null ? code.trim() : null;
    }

    /**
     * Detects the programming language of a code block.
     * <p>
     * Detection strategy:
     * 1. Check class attributes (language-java, hljs-java, etc.)
     * 2. Check data-lang attributes
     * 3. Analyze code content patterns
     * 4. Default to "java" for Spring documentation
     *
     * @param codeElement the code block element
     * @param code        the actual code content
     * @return the detected language name
     */
    public String detectLanguage(Element codeElement, String code) {
        if (codeElement == null) {
            return "java"; // Default for Spring docs
        }

        // Strategy 1: Check class attributes
        String language = detectLanguageFromClasses(codeElement);
        if (language != null) {
            return language;
        }

        // Strategy 2: Check data-lang attribute
        String dataLang = codeElement.attr("data-lang");
        if (!dataLang.isEmpty()) {
            return normalizeLanguageName(dataLang);
        }

        // Strategy 3: Check parent element classes (for nested structures)
        Element parent = codeElement.parent();
        if (parent != null) {
            language = detectLanguageFromClasses(parent);
            if (language != null) {
                return language;
            }
        }

        // Strategy 4: Analyze code content
        if (code != null && !code.isBlank()) {
            language = detectLanguageFromContent(code);
            if (language != null) {
                return language;
            }
        }

        // Default to Java for Spring documentation
        return "java";
    }

    /**
     * Detects language from CSS class attributes.
     *
     * @param element the element to check
     * @return the detected language or null
     */
    private String detectLanguageFromClasses(Element element) {
        String classAttr = element.className();
        if (classAttr.isEmpty()) {
            return null;
        }

        Matcher matcher = LANGUAGE_CLASS_PATTERN.matcher(classAttr);
        while (matcher.find()) {
            String lang = matcher.group(1).toLowerCase();
            lang = normalizeLanguageName(lang);

            // Validate it's a known language
            if (isKnownLanguage(lang)) {
                return lang;
            }
        }

        return null;
    }

    /**
     * Detects language from code content patterns.
     *
     * @param code the code content to analyze
     * @return the detected language or null
     */
    private String detectLanguageFromContent(String code) {
        // Check each language pattern
        for (Map.Entry<String, Pattern> entry : LANGUAGE_PATTERNS.entrySet()) {
            if (entry.getValue().matcher(code).find()) {
                return entry.getKey();
            }
        }

        return null;
    }

    /**
     * Normalizes language names to standard formats.
     *
     * @param lang the language name to normalize
     * @return the normalized language name
     */
    private String normalizeLanguageName(String lang) {
        if (lang == null) {
            return "java";
        }

        lang = lang.toLowerCase().trim();

        // Handle common aliases
        return switch (lang) {
            case "js" -> "javascript";
            case "ts" -> "typescript";
            case "yml" -> "yaml";
            case "sh" -> "bash";
            case "shell" -> "bash";
            case "dockerfile" -> "docker";
            case "props", "conf" -> "properties";
            case "kt" -> "kotlin";
            case "pom" -> "maven";
            default -> lang;
        };
    }

    /**
     * Checks if a language name is recognized.
     *
     * @param lang the language name
     * @return true if recognized
     */
    private boolean isKnownLanguage(String lang) {
        Set<String> knownLanguages = Set.of(
            "java", "kotlin", "xml", "yaml", "properties", "sql", "bash", "shell",
            "json", "groovy", "gradle", "maven", "docker", "html", "css",
            "javascript", "typescript", "python", "ruby", "go", "rust", "c", "cpp"
        );
        return knownLanguages.contains(lang);
    }

    /**
     * Extracts context and description from elements surrounding the code block.
     * Looks for headings, paragraphs, and captions near the code.
     *
     * @param codeElement the code block element
     * @return a context description
     */
    public String extractContext(Element codeElement) {
        if (codeElement == null) {
            return "";
        }

        StringBuilder context = new StringBuilder();

        // Strategy 1: Look for preceding heading
        Element heading = findPrecedingSibling(codeElement, "h1,h2,h3,h4,h5,h6");
        if (heading != null) {
            context.append(heading.text().trim());
        }

        // Strategy 2: Look for preceding paragraph
        Element paragraph = findPrecedingSibling(codeElement, "p");
        if (paragraph != null && context.length() < 500) {
            if (context.length() > 0) {
                context.append(". ");
            }
            String paraText = paragraph.text().trim();
            if (paraText.length() > 500) {
                paraText = paraText.substring(0, 497) + "...";
            }
            context.append(paraText);
        }

        // Strategy 3: Look for caption or title in parent
        Element parent = codeElement.parent();
        if (parent != null) {
            Element title = parent.selectFirst(".title,.caption,figcaption");
            if (title != null && context.length() < 200) {
                if (context.length() > 0) {
                    context.append(". ");
                }
                context.append(title.text().trim());
            }
        }

        String result = context.toString().trim();

        // Limit description length
        if (result.length() > MAX_DESCRIPTION_LENGTH) {
            result = result.substring(0, MAX_DESCRIPTION_LENGTH - 3) + "...";
        }

        return result.isEmpty() ? null : result;
    }

    /**
     * Finds a preceding sibling element matching the selector.
     *
     * @param element  the starting element
     * @param selector the CSS selector to match
     * @return the matching element or null
     */
    private Element findPrecedingSibling(Element element, String selector) {
        Element current = element;
        int maxLookback = 5; // Don't look too far back
        int count = 0;

        while (current != null && count < maxLookback) {
            current = current.previousElementSibling();
            if (current != null && current.is(selector)) {
                return current;
            }
            count++;
        }

        return null;
    }

    /**
     * Generates a descriptive title for the code example.
     *
     * @param codeElement the code element
     * @param code        the code content
     * @param language    the detected language
     * @return a generated title
     */
    private String generateTitle(Element codeElement, String code, String language) {
        // Try to extract title from context
        String context = extractContext(codeElement);
        if (context != null && !context.isEmpty()) {
            // Use first sentence as title
            int dotIndex = context.indexOf('.');
            if (dotIndex > 0 && dotIndex < 100) {
                return context.substring(0, dotIndex).trim();
            }
            if (context.length() <= 100) {
                return context;
            }
            return context.substring(0, 97) + "...";
        }

        // Fallback: Generate title from code content
        if (code.contains("class ")) {
            Pattern classPattern = Pattern.compile("class\\s+(\\w+)");
            Matcher matcher = classPattern.matcher(code);
            if (matcher.find()) {
                return matcher.group(1) + " - " + language + " example";
            }
        }

        // Default title
        return Character.toUpperCase(language.charAt(0)) + language.substring(1) + " Code Example";
    }

    /**
     * Generates tags for a code example based on content analysis.
     *
     * @param code     the code content
     * @param language the programming language
     * @param context  the context description
     * @return a list of tags
     */
    public List<String> generateTags(String code, String language, String context) {
        Set<String> tags = new HashSet<>();

        // Add language as primary tag
        tags.add(language);

        // Analyze code for known keywords
        if (code != null) {
            for (Map.Entry<String, List<String>> entry : KEYWORD_TAGS.entrySet()) {
                if (code.contains(entry.getKey())) {
                    tags.addAll(entry.getValue());
                }
            }

            // Add specific technology tags
            if (code.contains("MongoDB") || code.contains("MongoRepository")) {
                tags.add("mongodb");
            }
            if (code.contains("Redis") || code.contains("RedisTemplate")) {
                tags.add("redis");
            }
            if (code.contains("Kafka") || code.contains("@KafkaListener")) {
                tags.add("kafka");
            }
            if (code.contains("Security") || code.contains("@Secured")) {
                tags.add("security");
            }
            if (code.contains("async") || code.contains("@Async")) {
                tags.add("async");
            }
            if (code.contains("reactive") || code.contains("Mono") || code.contains("Flux")) {
                tags.add("reactive");
            }
        }

        // Analyze context for additional tags
        if (context != null) {
            String lowerContext = context.toLowerCase();
            if (lowerContext.contains("configuration")) {
                tags.add("configuration");
            }
            if (lowerContext.contains("example")) {
                tags.add("example");
            }
            if (lowerContext.contains("test")) {
                tags.add("testing");
            }
        }

        return new ArrayList<>(tags);
    }

    /**
     * Determines the category based on context and tags.
     *
     * @param context the context description
     * @param tags    the generated tags
     * @return a category name
     */
    private String determineCategoryFromContext(String context, List<String> tags) {
        // Priority-based category determination
        if (tags.contains("configuration")) {
            return "Configuration";
        }
        if (tags.contains("testing")) {
            return "Testing";
        }
        if (tags.contains("web") || tags.contains("rest")) {
            return "Web";
        }
        if (tags.contains("database") || tags.contains("jpa")) {
            return "Data Access";
        }
        if (tags.contains("security")) {
            return "Security";
        }
        if (tags.contains("reactive")) {
            return "Reactive";
        }

        // Analyze context
        if (context != null) {
            String lower = context.toLowerCase();
            if (lower.contains("configuration")) return "Configuration";
            if (lower.contains("controller")) return "Web";
            if (lower.contains("service")) return "Business Logic";
            if (lower.contains("repository")) return "Data Access";
            if (lower.contains("test")) return "Testing";
            if (lower.contains("security")) return "Security";
        }

        return "General";
    }

    /**
     * Generates a hash for code content to detect duplicates.
     *
     * @param code the code content
     * @return a hash string
     */
    private String generateCodeHash(String code) {
        return String.valueOf(code.hashCode());
    }

    /**
     * Extracts code examples from a documentation link by fetching and parsing its content.
     * This is a convenience method that can be used when you have the HTML content available.
     *
     * @param link     the documentation link
     * @param document the parsed HTML document
     * @return list of extracted code examples
     */
    @Transactional
    public List<CodeExample> extractAndSaveCodeExamples(DocumentationLink link, Document document) {
        return extractCodeExamples(document, link);
    }

    /**
     * Deletes all code examples associated with a specific documentation link.
     * Useful when re-indexing documentation.
     *
     * @param link the documentation link
     */
    @Transactional
    public void deleteCodeExamplesByLink(DocumentationLink link) {
        if (link == null || link.getVersion() == null) {
            return;
        }

        List<CodeExample> examples = codeExampleRepository.findByVersion(link.getVersion());
        List<CodeExample> toDelete = examples.stream()
            .filter(ex -> link.getUrl().equals(ex.getSourceUrl()))
            .toList();

        if (!toDelete.isEmpty()) {
            codeExampleRepository.deleteAll(toDelete);
            log.info("Deleted {} code examples for link: {}", toDelete.size(), link.getUrl());
        }
    }
}
