package com.ubique.prayer.communityunit.domain.converter;

import com.ubique.prayer.communityunit.domain.UnitType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class UnitTypeConverter implements AttributeConverter<UnitType, Integer> {
    @Override
    public Integer convertToDatabaseColumn(UnitType value) {
        return value == null ? null : value.getCode();
    }

    @Override
    public UnitType convertToEntityAttribute(Integer code) {
        return code == null ? null : UnitType.fromCode(code);
    }
}
