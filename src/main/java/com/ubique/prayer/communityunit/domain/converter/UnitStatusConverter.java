package com.ubique.prayer.communityunit.domain.converter;

import com.ubique.prayer.communityunit.domain.UnitStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class UnitStatusConverter implements AttributeConverter<UnitStatus, Integer> {
    @Override
    public Integer convertToDatabaseColumn(UnitStatus value) {
        return value == null ? null : value.getCode();
    }

    @Override
    public UnitStatus convertToEntityAttribute(Integer code) {
        return code == null ? null : UnitStatus.fromCode(code);
    }
}
