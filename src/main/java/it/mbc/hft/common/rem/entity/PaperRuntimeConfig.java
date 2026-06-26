package it.mbc.hft.common.rem.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "acdc_paper_runtime_config")
public class PaperRuntimeConfig extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    public StrategyProfile profile;

    @Column(name = "config_key", nullable = false, length = 80)
    public String configKey;

    @Column(nullable = false, length = 20)
    public String status;

    @Column(name = "fee_rate", nullable = false, precision = 19, scale = 8)
    public BigDecimal feeRate;

    @Column(name = "min_notional_quote", nullable = false, precision = 38, scale = 18)
    public BigDecimal minNotionalQuote;

    @Column(name = "min_trade_quote", nullable = false, precision = 38, scale = 18)
    public BigDecimal minTradeQuote;

    @Column(name = "step_size", nullable = false, precision = 38, scale = 18)
    public BigDecimal stepSize;

    @Column(name = "tick_size", nullable = false, precision = 38, scale = 18)
    public BigDecimal tickSize;

    @Column(name = "max_open_positions", nullable = false)
    public int maxOpenPositions;

    @Column(name = "low_balance_max_quote", nullable = false, precision = 38, scale = 18)
    public BigDecimal lowBalanceMaxQuote;

    @Column(name = "low_balance_step_quote", nullable = false, precision = 38, scale = 18)
    public BigDecimal lowBalanceStepQuote;

    @Column(name = "low_balance_start_percent", nullable = false, precision = 19, scale = 8)
    public BigDecimal lowBalanceStartPercent;

    @Column(name = "high_balance_trade_percent", nullable = false, precision = 19, scale = 8)
    public BigDecimal highBalanceTradePercent;

    @Column(name = "trade_budget_percent", nullable = false, precision = 19, scale = 8)
    public BigDecimal tradeBudgetPercent;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;
}
