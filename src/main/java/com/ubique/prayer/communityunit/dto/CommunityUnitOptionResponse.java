package com.ubique.prayer.communityunit.dto;

import com.ubique.prayer.communityunit.domain.CellCategory;

public record CommunityUnitOptionResponse(
        Long unitId,
        String name,
        String cellCategory
) {}