package com.dnd.campaignmanager.campaign;

import com.dnd.campaignmanager.common.TimestampedEntity;
import com.dnd.campaignmanager.gamesystem.SheetField;
import com.dnd.campaignmanager.gamesystem.SheetTemplate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BuildConstraint extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConstraintType type;

    @Column(length = 60)
    private String section;

    @Column(length = 64)
    private String fieldKey;

    @Column(name = "limit_value")
    private Integer limit;

    @Column(length = 120)
    private String text;

    BuildConstraint(Campaign campaign, ConstraintType type, String section, String fieldKey, Integer limit, String text) {
        this.campaign = campaign;
        this.type = type;
        this.section = section;
        this.fieldKey = fieldKey;
        this.limit = limit;
        this.text = text;
    }

    public List<SheetField> fieldsInScope(SheetTemplate template) {
        return template.sections().stream()
                .filter(s -> section == null || s.name().equals(section))
                .flatMap(s -> s.fields().stream())
                .filter(f -> fieldKey == null || f.key().equals(fieldKey))
                .filter(f -> type.getAppliesTo().contains(f.type()))
                .toList();
    }

    public String describe(SheetTemplate template) {
        String scope = scopeLabel(template);
        return switch (type) {
            case MAX_VALUE -> scope + ": at most " + limit;
            case MIN_VALUE -> scope + ": at least " + limit;
            case MAX_TOTAL -> scope + " total: at most " + limit;
            case MAX_ENTRIES -> scope + ": at most " + limit + " entries";
            case MAX_REPEATS -> scope + ": the same entry at most " + limit + (limit == 1 ? " time" : " times");
            case FORBIDDEN_TEXT -> scope + ": may not contain \"" + text + "\"";
        };
    }

    public List<String> violations(SheetTemplate template, Map<String, String> sheet) {
        List<SheetField> fields = fieldsInScope(template);
        List<String> violations = new ArrayList<>();
        switch (type) {
            case MAX_VALUE -> fields.forEach(field -> {
                int value = numberOrZero(sheet, field);
                if (value > limit) {
                    violations.add(field.label() + " is " + value + ", limit is " + limit);
                }
            });
            case MIN_VALUE -> fields.forEach(field -> {
                int value = numberOrZero(sheet, field);
                if (value < limit) {
                    violations.add(field.label() + " is " + value + ", minimum is " + limit);
                }
            });
            case MAX_TOTAL -> {
                int total = fields.stream().mapToInt(field -> numberOrZero(sheet, field)).sum();
                if (total > limit) {
                    violations.add(scopeLabel(template) + " total is " + total + ", limit is " + limit);
                }
            }
            case MAX_ENTRIES -> fields.forEach(field -> {
                int count = entries(sheet, field).size();
                if (count > limit) {
                    violations.add(field.label() + " has " + count + " entries, limit is " + limit);
                }
            });
            case MAX_REPEATS -> fields.forEach(field -> countEntries(sheet, field).forEach((entry, count) -> {
                if (count > limit) {
                    violations.add(field.label() + ": \"" + entry + "\" appears " + count + " times, limit is " + limit);
                }
            }));
            case FORBIDDEN_TEXT -> fields.forEach(field -> {
                String value = sheet.getOrDefault(field.key(), "");
                if (value.toLowerCase(Locale.ROOT).contains(text.toLowerCase(Locale.ROOT))) {
                    violations.add(field.label() + " may not contain \"" + text + "\"");
                }
            });
        }
        return violations;
    }

    private String scopeLabel(SheetTemplate template) {
        if (fieldKey != null) {
            SheetField field = template.fieldsByKey().get(fieldKey);
            return field == null ? fieldKey : field.label();
        }
        if (section != null) {
            return type == ConstraintType.MAX_TOTAL ? section : "Any " + section + " field";
        }
        return type == ConstraintType.MAX_TOTAL ? "All stats" : "Any field";
    }

    private static int numberOrZero(Map<String, String> sheet, SheetField field) {
        String value = sheet.get(field.key());
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static List<String> entries(Map<String, String> sheet, SheetField field) {
        String value = sheet.getOrDefault(field.key(), "");
        return value.lines()
                .flatMap(line -> Arrays.stream(line.split(",")))
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .toList();
    }

    private static Map<String, Integer> countEntries(Map<String, String> sheet, SheetField field) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String entry : entries(sheet, field)) {
            counts.merge(entry.toLowerCase(Locale.ROOT), 1, Integer::sum);
        }
        return counts;
    }
}
