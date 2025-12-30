package com.spring.mcp.model.entity;

import com.spring.mcp.model.enums.VersionState;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a version of a Spring project
 */
@Entity
@Table(name = "project_versions", uniqueConstraints = {
    @UniqueConstraint(name = "unique_project_version", columnNames = {"project_id", "version"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"project", "documentationLinks", "codeExamples"})
@EqualsAndHashCode(of = {"id", "version"})
public class ProjectVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private SpringProject project;

    @Column(nullable = false, length = 50)
    private String version;

    @Column(name = "major_version", nullable = false)
    private Integer majorVersion;

    @Column(name = "minor_version", nullable = false)
    private Integer minorVersion;

    @Column(name = "patch_version")
    private Integer patchVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private VersionState state;

    @Column(name = "is_latest")
    @Builder.Default
    private Boolean isLatest = false;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "oss_support_end")
    private LocalDate ossSupportEnd;

    @Column(name = "enterprise_support_end")
    private LocalDate enterpriseSupportEnd;

    @Column(name = "reference_doc_url", length = 500)
    private String referenceDocUrl;

    @Column(name = "api_doc_url", length = 500)
    private String apiDocUrl;

    @Column(name = "status", length = 20)
    private String status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DocumentationLink> documentationLinks = new ArrayList<>();

    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CodeExample> codeExamples = new ArrayList<>();


    public boolean getVisible() {return status != null;}
    /**
     * Check if this version is end-of-life (enterprise support has ended)
     */
    public boolean isEndOfLife() {
        return enterpriseSupportEnd != null && LocalDate.now().isAfter(enterpriseSupportEnd);
    }

    /**
     * Get formatted version string
     */
    public String getFormattedVersion() {
        return "";
    }

    /**
     * Check if this version should show a red flag (support has ended).
     * Red flag indicates that the version is no longer supported based on the subscription type.
     *
     * @param enterpriseSubscriptionEnabled true if enterprise subscription is enabled, false for OSS only
     * @return true if support has ended (red flag), false if still supported (green flag)
     */
    public boolean getRedFlag(boolean enterpriseSubscriptionEnabled) {
        LocalDate supportEndDate = enterpriseSubscriptionEnabled ? enterpriseSupportEnd : ossSupportEnd;

        if (supportEndDate == null) {
            return false; // No end date means still supported (green)
        }

        LocalDate today = LocalDate.now();
        return today.isAfter(supportEndDate) || today.isEqual(supportEndDate); // Red if today >= support end
    }
}
