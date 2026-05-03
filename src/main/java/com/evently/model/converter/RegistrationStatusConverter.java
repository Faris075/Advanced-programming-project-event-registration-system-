package com.evently.model.converter;

import com.evently.model.RegistrationStatus;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class RegistrationStatusConverter implements AttributeConverter<RegistrationStatus, String> {

    @Override
    public String convertToDatabaseColumn(RegistrationStatus attribute) {
        if (attribute == null) return null;
        return attribute.name().toLowerCase();
    }

    @Override
    public RegistrationStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return RegistrationStatus.valueOf(dbData.toUpperCase());
    }
}
