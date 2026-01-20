package com.example.demo.dispatch.converter;

import com.example.demo.dispatch.model.json.AutoPlanningResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AutoPlanningResultConverter implements AttributeConverter<AutoPlanningResult, String> {
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(AutoPlanningResult attribute) {
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert AutoPlanningResult to JSON string", e);
        }
    }

    @Override
    public AutoPlanningResult convertToEntityAttribute(String dbData) {
        try {
            return objectMapper.readValue(dbData, AutoPlanningResult.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON string to AutoPlanningResult", e);
        }
    }
}
