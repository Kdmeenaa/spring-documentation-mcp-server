package com.spring.mcp.model.dto.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for findProjectsByUseCase tool
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectsByUseCaseResponse {
    private String useCase;
    private Integer totalFound;
    private Long executionTimeMs;
    private List<ProjectInfo> projects;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProjectInfo {
        private String name;
        private String slug;
        private String description;
        private String homepage;
        private String github;
    }
}
