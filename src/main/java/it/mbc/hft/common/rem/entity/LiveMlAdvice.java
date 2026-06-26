package it.mbc.hft.common.rem.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "acdc_live_ml_advice")
public class LiveMlAdvice extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    public StrategyProfile profile;

    @Column(nullable = false, length = 40)
    public String symbol;

    @Column(name = "rule_id", nullable = false)
    public Long ruleId;

    @Column(name = "scope_type", nullable = false, length = 20)
    public String scopeType;

    @Column(name = "scope_key", nullable = false, length = 80)
    public String scopeKey;

    @Column(name = "rule_key", nullable = false, length = 120)
    public String ruleKey;

    @Column(name = "promotion_class", nullable = false, length = 40)
    public String promotionClass;

    @Column(name = "source_generation_id", length = 120)
    public String sourceGenerationId;

    @Column(name = "advice_source", nullable = false, length = 40)
    public String adviceSource;

    @Column(name = "feature_json", nullable = false)
    public String featureJson;

    @Column(name = "advice_json", nullable = false)
    public String adviceJson;

    @Column(nullable = false, precision = 38, scale = 18)
    public BigDecimal score;

    @Column(name = "validation_samples", nullable = false)
    public int validationSamples;

    @Column(name = "validation_profit_rate", nullable = false, precision = 38, scale = 18)
    public BigDecimal validationProfitRate;

    @Column(name = "validation_avg_net_return", nullable = false, precision = 38, scale = 18)
    public BigDecimal validationAvgNetReturn;

    @Column(name = "advice_valid_from", nullable = false)
    public LocalDateTime adviceValidFrom;

    @Column(name = "advice_valid_until", nullable = false)
    public LocalDateTime adviceValidUntil;

    @Column(name = "used_at")
    public LocalDateTime usedAt;

    @Column(name = "used_execution_id")
    public Long usedExecutionId;

    @Column(nullable = false, length = 20)
    public String status;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;
}
