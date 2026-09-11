package com.dnd.campaignmanager.character;

import com.dnd.campaignmanager.common.InvalidRequestException;
import com.dnd.campaignmanager.gamesystem.GameSystem;

import java.util.LinkedHashMap;
import java.util.Map;

final class SheetValues {

    private SheetValues() {
    }

    static Map<String, String> clean(GameSystem system, Map<String, String> rawValues) {
        Map<String, String> values = withoutBlanks(rawValues);
        Map<String, String> errors = system.sheetTemplate().validate(values);
        if (!errors.isEmpty()) {
            throw new InvalidRequestException("Character sheet has invalid values", errors);
        }
        return values;
    }

    private static Map<String, String> withoutBlanks(Map<String, String> rawValues) {
        Map<String, String> values = new LinkedHashMap<>();
        if (rawValues == null) {
            return values;
        }
        rawValues.forEach((key, value) -> {
            if (key != null && value != null && !value.isBlank()) {
                values.put(key.trim(), value.trim());
            }
        });
        return values;
    }
}
