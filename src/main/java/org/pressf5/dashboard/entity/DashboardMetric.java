
package org.pressf5.dashboard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dashboard_metric")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetric {

    @Id
    @Column(name = "metric_key", nullable = false, length = 80)
    private String metricKey;

    @Column(name = "label", nullable = false, length = 300)
    private String label;

    @Column(name = "metric_order", nullable = false)
    private Integer metricOrder;

    @Column(name = "category_key", nullable = false, length = 80)
    private String categoryKey;

    @Column(name = "category_label", nullable = false, length = 200)
    private String categoryLabel;

    @Column(name = "unit", length = 30)
    private String unit;

    @Column(name = "is_percentage", nullable = false)
    private boolean percentage;
}
