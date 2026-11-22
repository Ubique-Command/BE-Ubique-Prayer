package com.ubique.prayer.communityunit.repository;

import com.ubique.prayer.communityunit.domain.CommunityUnit;
import com.ubique.prayer.communityunit.domain.UnitStatus;
import com.ubique.prayer.communityunit.domain.UnitType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityUnitRepository
        extends JpaRepository<CommunityUnit, Long> {

    List<CommunityUnit> findAllByUnitTypeAndStatusOrderByUnitNameAsc(
            UnitType unitType,
            UnitStatus status
    );
}