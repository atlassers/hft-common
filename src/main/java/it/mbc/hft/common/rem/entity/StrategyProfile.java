package it.mbc.hft.common.rem.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "acdc_strategy_profile")
public class StrategyProfile extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "profile_key", nullable = false, unique = true, length = 80)
    public String profileKey;

    @Column(nullable = false)
    public String description;

    @Column(nullable = false, length = 20)
    public String status;

    @Column(name = "default_execution_mode", nullable = false, length = 10)
    public String defaultExecutionMode;

    @Column(name = "min_validation_trades", nullable = false)
    public int minValidationTrades;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;
}
