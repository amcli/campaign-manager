package com.dnd.campaignmanager.character;

public record CharacterOption(Long id, String name) {

    public static CharacterOption from(PlayerCharacter character) {
        return new CharacterOption(character.getId(), character.getName());
    }
}
