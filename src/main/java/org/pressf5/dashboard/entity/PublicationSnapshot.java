
package org.pressf5.dashboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "publication_snapshot")
@Getter
@Setter
@NoArgsConstructor
public class PublicationSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_date", nullable = false, unique = true)
    private LocalDate snapshotDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_import_id")
    private ImportBatch sourceImport;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
