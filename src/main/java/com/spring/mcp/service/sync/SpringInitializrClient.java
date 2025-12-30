package com.spring.mcp.service.sync;

import com.spring.mcp.model.dto.springio.SpringInitializrMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Client service for fetching data from Spring Initializr API.
 * Retrieves metadata about Spring Boot versions and dependencies.
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-08
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SpringInitializrClient {

    private static final String SPRING_INITIALIZR_URL = "https://start.spring.io";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final WebClient.Builder webClientBuilder;

    /**
     * Fetch Spring Initializr metadata.
     *
     * @return SpringInitializrMetadata containing boot versions and dependencies
     */
    public SpringInitializrMetadata fetchMetadata() {
        log.debug("Fetching metadata from Spring Initializr");

        try {
            WebClient webClient = webClientBuilder
                .baseUrl(SPRING_INITIALIZR_URL)
                .build();

            Mono<SpringInitializrMetadata> response = webClient.get()
                .uri(uriBuilder -> uriBuilder.build())
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(SpringInitializrMetadata.class)
                .timeout(TIMEOUT);

            SpringInitializrMetadata metadata = response.block();

            if (metadata != null && metadata.getBootVersion() != null) {
                log.info("Successfully fetched Spring Initializr metadata with {} boot versions",
                    metadata.getBootVersion().getValues().size());
            } else {
                log.warn("Received null or incomplete metadata from Spring Initializr");
            }

            return metadata;
        } catch (Exception e) {
            log.error("Error fetching Spring Initializr metadata", e);
            throw new RuntimeException("Failed to fetch Spring Initializr metadata: " + e.getMessage(), e);
        }
    }
}
