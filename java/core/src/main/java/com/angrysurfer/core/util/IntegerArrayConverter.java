package com.angrysurfer.core.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.stream.Collectors;

@Converter
public class IntegerArrayConverter implements AttributeConverter<Integer[], String> {
    private static final String DELIMITER = ",";

    @Override
    public String convertToDatabaseColumn(Integer[] attribute) {
        if (attribute == null || attribute.length == 0) {
            return null;
        }
        return Arrays.stream(attribute)
                .map(String::valueOf)
                .collect(Collectors.joining(DELIMITER));
    }

    @Override
    public Integer[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new Integer[0];
        }
        return Arrays.stream(dbData.split(DELIMITER))
                .map(Integer::valueOf)
                .toArray(Integer[]::new);
    }
}
