
package org.pressf5.dashboard.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(
        name = "msu_statistics",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_msu_statistics_snapshot_msu",
                        columnNames = {"snapshot_id", "msu_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_msu_statistics_snapshot",
                        columnList = "snapshot_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MsuStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private StatisticsSnapshot snapshot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "msu_id", nullable = false)
    private Msu msu;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metrics", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metrics;
}
