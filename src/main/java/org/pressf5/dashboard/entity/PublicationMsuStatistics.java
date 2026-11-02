
package org.pressf5.dashboard.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "publication_msu_statistics",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_publication_snapshot_msu",
                        columnNames = {"snapshot_id", "msu_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_publication_snapshot_date",
                        columnList = "snapshot_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PublicationMsuStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private PublicationSnapshot snapshot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "msu_id", nullable = false)
    private Msu msu;

    @Column(name = "source_code", length = 40)
    private String sourceCode;

    @Column(name = "social_publications")
    private Long socialPublications;

    @Column(name = "website_publications")
    private Long websitePublications;

    @Column(name = "parent_chat_publications")
    private Long parentChatPublications;

    @Column(name = "thematic_videos")
    private Long thematicVideos;

    @Column(name = "publication_reach")
    private Long publicationReach;

    @Column(name = "organizations_percent", precision = 6, scale = 2)
    private BigDecimal organizationsPercent;
}
