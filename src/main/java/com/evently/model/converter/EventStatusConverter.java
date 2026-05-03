package com.evently.model.converter;

import com.evently.model.EventStatus;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class EventStatusConverter implements AttributeConverter<EventStatus, String> {

    @Override
    public String convertToDatabaseColumn(EventStatus attribute) {
        if (attribute == null) return null;
        return attribute.name().toLowerCase();
    }

    @Override
    public EventStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return EventStatus.valueOf(dbData.toUpperCase());
    }
}
