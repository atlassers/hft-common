package it.mbc.hft.common.rem.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "acdc_outcome_signature")
public class OutcomeSignature extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    public StrategyProfile profile;

    @Column(name = "scope_type", nullable = false, length = 20)
    public String scopeType;

    @Column(name = "scope_key", nullable = false, length = 80)
    public String scopeKey;

    @Column(name = "feature_key", nullable = false, length = 120)
    public String featureKey;

    @Column(name = "min_value", precision = 38, scale = 18)
    public BigDecimal minValue;

    @Column(name = "max_value", precision = 38, scale = 18)
    public BigDecimal maxValue;

    @Column(name = "train_samples", nullable = false)
    public int trainSamples;

    @Column(name = "validation_samples", nullable = false)
    public int validationSamples;

    @Column(name = "train_profit_rate", nullable = false, precision = 38, scale = 18)
    public BigDecimal trainProfitRate;

    @Column(name = "validation_profit_rate", nullable = false, precision = 38, scale = 18)
    public BigDecimal validationProfitRate;

    @Column(name = "validation_avg_net_return", nullable = false, precision = 38, scale = 18)
    public BigDecimal validationAvgNetReturn;

    @Column(nullable = false, length = 20)
    public String status;

    @Column(name = "metadata_json")
    public String metadataJson;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;
}
