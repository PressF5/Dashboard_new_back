
package org.pressf5.dashboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "educational_organization",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_organization_msu_name",
                        columnNames = {"msu_id", "name"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class EducationalOrganization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "msu_id", nullable = false)
    private Msu msu;

    @Column(name = "name", nullable = false, length = 300)
    private String name;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
