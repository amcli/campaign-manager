package com.dnd.campaignmanager.gamesystem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record SheetTemplate(List<SheetSection> sections) {

    private static final int TEXT_MAX_LENGTH = 255;
    private static final int LONG_TEXT_MAX_LENGTH = 5000;

    public Map<String, SheetField> fieldsByKey() {
        Map<String, SheetField> byKey = new LinkedHashMap<>();
        for (SheetSection section : sections) {
            for (SheetField field : section.fields()) {
                byKey.put(field.key(), field);
            }
        }
        return byKey;
    }

    public Map<String, String> validate(Map<String, String> values) {
        Map<String, SheetField> fields = fieldsByKey();
        Map<String, String> errors = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            SheetField field = fields.get(key);
            if (field == null) {
                errors.put(key, "Unknown field for this game system");
                return;
            }
            String error = validateValue(field, value);
            if (error != null) {
                errors.put(key, error);
            }
        });
        return errors;
    }

    private static String validateValue(SheetField field, String value) {
        return switch (field.type()) {
            case NUMBER -> isInteger(value) ? null : "Must be a whole number";
            case BOOLEAN -> value.equals("true") || value.equals("false") ? null : "Must be true or false";
            case TEXT -> value.length() <= TEXT_MAX_LENGTH ? null : "Must be at most " + TEXT_MAX_LENGTH + " characters";
            case LONG_TEXT -> value.length() <= LONG_TEXT_MAX_LENGTH ? null : "Must be at most " + LONG_TEXT_MAX_LENGTH + " characters";
        };
    }

    private static boolean isInteger(String value) {
        try {
            Integer.parseInt(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<SheetSection> sections = new ArrayList<>();
        private String currentSection;
        private List<SheetField> currentFields;

        private Builder() {
        }

        public Builder section(String name) {
            finishSection();
            currentSection = name;
            currentFields = new ArrayList<>();
            return this;
        }

        public Builder text(String key, String label) {
            return field(key, label, FieldType.TEXT);
        }

        public Builder longText(String key, String label) {
            return field(key, label, FieldType.LONG_TEXT);
        }

        public Builder number(String key, String label) {
            return field(key, label, FieldType.NUMBER);
        }

        public Builder flag(String key, String label) {
            return field(key, label, FieldType.BOOLEAN);
        }

        public SheetTemplate build() {
            finishSection();
            return new SheetTemplate(List.copyOf(sections));
        }

        private Builder field(String key, String label, FieldType type) {
            if (currentFields == null) {
                throw new IllegalStateException("Call section() before adding fields");
            }
            currentFields.add(new SheetField(key, label, type));
            return this;
        }

        private void finishSection() {
            if (currentSection != null) {
                sections.add(new SheetSection(currentSection, List.copyOf(currentFields)));
            }
        }
    }
}
