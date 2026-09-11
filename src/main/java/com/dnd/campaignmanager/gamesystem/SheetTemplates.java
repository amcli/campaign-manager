package com.dnd.campaignmanager.gamesystem;

import java.util.EnumMap;
import java.util.Map;

final class SheetTemplates {

    private static final Map<GameSystem, SheetTemplate> TEMPLATES = new EnumMap<>(GameSystem.class);

    static {
        TEMPLATES.put(GameSystem.DND_5E, dnd5e());
        TEMPLATES.put(GameSystem.PATHFINDER_2E, pathfinder2e());
        TEMPLATES.put(GameSystem.MUTANTS_AND_MASTERMINDS_3E, mutantsAndMasterminds3e());
        TEMPLATES.put(GameSystem.DELTA_GREEN, deltaGreen());
        TEMPLATES.put(GameSystem.CALL_OF_CTHULHU_7E, callOfCthulhu7e());
        TEMPLATES.put(GameSystem.GENERIC, generic());
    }

    private SheetTemplates() {
    }

    static SheetTemplate forSystem(GameSystem system) {
        return TEMPLATES.get(system);
    }

    private static SheetTemplate dnd5e() {
        return SheetTemplate.builder()
                .section("Identity")
                .text("characterClass", "Class")
                .text("subclass", "Subclass")
                .text("race", "Race")
                .text("background", "Background")
                .text("alignment", "Alignment")
                .number("level", "Level")
                .section("Ability Scores")
                .number("strength", "Strength")
                .number("dexterity", "Dexterity")
                .number("constitution", "Constitution")
                .number("intelligence", "Intelligence")
                .number("wisdom", "Wisdom")
                .number("charisma", "Charisma")
                .section("Combat")
                .number("hitPoints", "Hit Points")
                .number("maxHitPoints", "Max Hit Points")
                .number("armorClass", "Armor Class")
                .number("speed", "Speed")
                .number("initiative", "Initiative")
                .number("proficiencyBonus", "Proficiency Bonus")
                .flag("inspiration", "Inspiration")
                .section("Details")
                .longText("featuresAndTraits", "Features & Traits")
                .longText("equipment", "Equipment")
                .longText("spells", "Spells")
                .longText("backstory", "Backstory")
                .build();
    }

    private static SheetTemplate pathfinder2e() {
        return SheetTemplate.builder()
                .section("Identity")
                .text("ancestry", "Ancestry")
                .text("heritage", "Heritage")
                .text("background", "Background")
                .text("characterClass", "Class")
                .number("level", "Level")
                .text("deity", "Deity")
                .section("Attributes")
                .number("strength", "Strength")
                .number("dexterity", "Dexterity")
                .number("constitution", "Constitution")
                .number("intelligence", "Intelligence")
                .number("wisdom", "Wisdom")
                .number("charisma", "Charisma")
                .section("Combat")
                .number("hitPoints", "Hit Points")
                .number("maxHitPoints", "Max Hit Points")
                .number("armorClass", "Armor Class")
                .number("speed", "Speed")
                .number("perception", "Perception")
                .number("heroPoints", "Hero Points")
                .section("Saving Throws")
                .number("fortitude", "Fortitude")
                .number("reflex", "Reflex")
                .number("will", "Will")
                .section("Details")
                .longText("feats", "Feats")
                .longText("equipment", "Equipment")
                .longText("spells", "Spells")
                .longText("backstory", "Backstory")
                .build();
    }

    private static SheetTemplate mutantsAndMasterminds3e() {
        return SheetTemplate.builder()
                .section("Identity")
                .text("heroName", "Hero Name")
                .text("secretIdentity", "Secret Identity")
                .number("powerLevel", "Power Level")
                .number("powerPoints", "Power Points")
                .number("heroPoints", "Hero Points")
                .section("Abilities")
                .number("strength", "Strength")
                .number("stamina", "Stamina")
                .number("agility", "Agility")
                .number("dexterity", "Dexterity")
                .number("fighting", "Fighting")
                .number("intellect", "Intellect")
                .number("awareness", "Awareness")
                .number("presence", "Presence")
                .section("Defenses")
                .number("dodge", "Dodge")
                .number("parry", "Parry")
                .number("fortitude", "Fortitude")
                .number("toughness", "Toughness")
                .number("will", "Will")
                .section("Details")
                .longText("powers", "Powers")
                .longText("advantages", "Advantages")
                .longText("skills", "Skills")
                .longText("equipment", "Equipment")
                .longText("complications", "Complications")
                .longText("origin", "Origin Story")
                .build();
    }

    private static SheetTemplate deltaGreen() {
        return SheetTemplate.builder()
                .section("Identity")
                .text("profession", "Profession")
                .text("employer", "Employer")
                .text("nationality", "Nationality")
                .number("age", "Age")
                .section("Statistics")
                .number("strength", "Strength")
                .number("constitution", "Constitution")
                .number("dexterity", "Dexterity")
                .number("intelligence", "Intelligence")
                .number("power", "Power")
                .number("charisma", "Charisma")
                .section("Derived")
                .number("hitPoints", "Hit Points")
                .number("willpowerPoints", "Willpower Points")
                .number("sanity", "Sanity")
                .number("breakingPoint", "Breaking Point")
                .section("Details")
                .longText("skills", "Skills")
                .longText("bonds", "Bonds")
                .longText("motivations", "Motivations & Disorders")
                .longText("gear", "Gear")
                .longText("background", "Background")
                .build();
    }

    private static SheetTemplate callOfCthulhu7e() {
        return SheetTemplate.builder()
                .section("Identity")
                .text("occupation", "Occupation")
                .number("age", "Age")
                .text("residence", "Residence")
                .text("birthplace", "Birthplace")
                .section("Characteristics")
                .number("strength", "Strength")
                .number("constitution", "Constitution")
                .number("size", "Size")
                .number("dexterity", "Dexterity")
                .number("appearance", "Appearance")
                .number("intelligence", "Intelligence")
                .number("power", "Power")
                .number("education", "Education")
                .section("Derived")
                .number("hitPoints", "Hit Points")
                .number("magicPoints", "Magic Points")
                .number("sanity", "Sanity")
                .number("luck", "Luck")
                .section("Details")
                .longText("skills", "Skills")
                .longText("gear", "Gear")
                .longText("backstory", "Backstory")
                .build();
    }

    private static SheetTemplate generic() {
        return SheetTemplate.builder()
                .section("Identity")
                .text("concept", "Concept")
                .number("level", "Level / Tier")
                .section("Details")
                .longText("attributes", "Attributes")
                .longText("skills", "Skills & Abilities")
                .longText("gear", "Gear")
                .longText("notes", "Notes")
                .build();
    }
}
