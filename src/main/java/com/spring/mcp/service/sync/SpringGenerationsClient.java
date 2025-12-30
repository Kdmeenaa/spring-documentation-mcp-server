package com.spring.mcp.service.sync;

import com.spring.mcp.model.dto.springio.SpringGenerationsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Client service for fetching data from Spring Generations API.
 * Retrieves comprehensive mapping of Spring Boot versions to compatible Spring project versions.
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-08
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SpringGenerationsClient {

    private static final String SPRING_GENERATIONS_URL = "https://spring.io/page-data/projects/generations/page-data.json";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final WebClient.Builder webClientBuilder;

    /**
     * Fetch Spring Generations data containing all Spring Boot versions
     * and their compatible Spring project versions.
     *
     * @return SpringGenerationsResponse containing all mappings
     */
    public SpringGenerationsResponse fetchGenerations() {
        log.debug("Fetching Spring Generations data from {}", SPRING_GENERATIONS_URL);

        try {
            WebClient webClient = webClientBuilder
                .baseUrl("https://spring.io")
                .build();

            Mono<SpringGenerationsResponse> response = webClient.get()
                .uri("/page-data/projects/generations/page-data.json")
                .retrieve()
                .bodyToMono(SpringGenerationsResponse.class)
                .timeout(TIMEOUT);

            SpringGenerationsResponse data = response.block();

            if (data != null && data.getResult() != null &&
                data.getResult().getData() != null &&
                data.getResult().getData().getAllSpringBootGeneration() != null) {

                int bootVersionsCount = data.getResult().getData()
                    .getAllSpringBootGeneration().getNodes().size();

                log.info("Successfully fetched Spring Generations data with {} Spring Boot versions",
                    bootVersionsCount);
            } else {
                log.warn("Received null or incomplete data from Spring Generations API");
            }

            return data;
        } catch (Exception e) {
            log.error("Error fetching Spring Generations data", e);
            throw new RuntimeException("Failed to fetch Spring Generations data: " + e.getMessage(), e);
        }
    }
}
