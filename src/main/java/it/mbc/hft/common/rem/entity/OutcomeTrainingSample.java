package it.mbc.hft.common.rem.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "acdc_outcome_training_sample")
public class OutcomeTrainingSample extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    public StrategyProfile profile;

    @Column(nullable = false, length = 40)
    public String symbol;

    @Column(name = "observed_at", nullable = false)
    public LocalDateTime observedAt;

    @Column(name = "split_name", nullable = false, length = 20)
    public String splitName;

    @Column(name = "source_bucket", nullable = false, length = 80)
    public String sourceBucket;

    @Column(name = "feature_json", nullable = false)
    public String featureJson;

    @Column(name = "entry_price", nullable = false, precision = 38, scale = 18)
    public BigDecimal entryPrice;

    @Column(name = "horizon_seconds", nullable = false)
    public int horizonSeconds;

    @Column(name = "future_points", nullable = false)
    public int futurePoints;

    @Column(name = "max_net_return", nullable = false, precision = 38, scale = 18)
    public BigDecimal maxNetReturn;

    @Column(name = "min_net_return", nullable = false, precision = 38, scale = 18)
    public BigDecimal minNetReturn;

    @Column(name = "end_net_return", nullable = false, precision = 38, scale = 18)
    public BigDecimal endNetReturn;

    @Column(nullable = false, length = 20)
    public String label;

    @Column(nullable = false, length = 20)
    public String status;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;
}

