package com.ubique.prayer.communityunit.service;

import java.util.List;

import com.ubique.prayer.communityunit.domain.CommunityUnit;
import com.ubique.prayer.communityunit.domain.UnitStatus;
import com.ubique.prayer.communityunit.domain.UnitType;
import com.ubique.prayer.communityunit.dto.CommunityUnitOptionResponse;
import com.ubique.prayer.communityunit.repository.CommunityUnitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CommunityUnitService {

    private static final Logger log =
            LoggerFactory.getLogger(CommunityUnitService.class);
    private final CommunityUnitRepository communityUnitRepository;

    public CommunityUnitService(CommunityUnitRepository communityUnitRepository) {
        this.communityUnitRepository = communityUnitRepository;
    }

    public List<CommunityUnitOptionResponse> getActiveCells() {
        log.debug("CommunityUnitService.getActiveCells start");

        List<CommunityUnit> communityUnits = communityUnitRepository.findAllByUnitTypeAndStatusOrderByUnitNameAsc(UnitType.CELL, UnitStatus.ACTIVE);

        return communityUnits.stream()
                .map(this::toOptionResponse)
                .toList();
    }

    private CommunityUnitOptionResponse toOptionResponse(CommunityUnit unit) {
        return new CommunityUnitOptionResponse(
                unit.getUnitId(),
                unit.getUnitName(),
                unit.getUnitCategory()
        );
    }
}