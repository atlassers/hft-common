package it.mbc.hft.common.rem.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "acdc_symbol_family_rule")
public class SymbolFamilyRule extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    public StrategyProfile profile;

    @Column(name = "rule_key", nullable = false, length = 120)
    public String ruleKey;

    @Column(name = "family_key", nullable = false, length = 80)
    public String familyKey;

    @Column(name = "match_type", nullable = false, length = 40)
    public String matchType;

    @Column(name = "match_value", nullable = false, length = 500)
    public String matchValue;

    @Column(nullable = false, length = 20)
    public String status;

    @Column(nullable = false)
    public int priority;

    @Column(name = "metadata_json")
    public String metadataJson;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;
}
