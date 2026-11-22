package com.ubique.prayer.communityunit.domain;

import com.ubique.prayer.communityunit.domain.converter.UnitStatusConverter;
import com.ubique.prayer.communityunit.domain.converter.UnitTypeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "community_units")
public class CommunityUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "unit_id")
    private Long unitId;

    @Convert(converter = UnitTypeConverter.class)
    @Column(name = "unit_type", nullable = false)
    private UnitType unitType;

    @Column(name = "unit_name", nullable = false, length = 100)
    private String unitName;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_category", length = 1)
    private CellCategory unitCategory;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Convert(converter = UnitStatusConverter.class)
    @Column(name = "status", nullable = false)
    private UnitStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected CommunityUnit() {
    }

    public Long getUnitId() {
        return unitId;
    }

    public String getUnitName() {
        return unitName;
    }

    public String getUnitCategory() {
        return unitCategory == null ? null : unitCategory.name();
    }
}
