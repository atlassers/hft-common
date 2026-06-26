package it.mbc.hft.common.rem.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "acdc_shared_runtime_config")
public class SharedRuntimeConfig extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "config_key", nullable = false, length = 120)
    public String configKey;

    @Column(name = "config_value", nullable = false, length = 500)
    public String configValue;

    @Column(name = "value_type", nullable = false, length = 20)
    public String valueType;

    @Column(length = 500)
    public String description;

    @Column(nullable = false, length = 20)
    public String status;

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;
}
