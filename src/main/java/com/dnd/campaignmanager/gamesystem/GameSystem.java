package com.dnd.campaignmanager.gamesystem;

import lombok.Getter;

@Getter
public enum GameSystem {
    DND_5E("Dungeons & Dragons 5e", "Dungeon Master"),
    PATHFINDER_2E("Pathfinder 2e", "Game Master"),
    MUTANTS_AND_MASTERMINDS_3E("Mutants & Masterminds 3e", "Gamemaster"),
    DELTA_GREEN("Delta Green", "Handler"),
    CALL_OF_CTHULHU_7E("Call of Cthulhu 7e", "Keeper"),
    GENERIC("Generic / Homebrew", "Game Master");

    private final String displayName;
    private final String gameMasterTitle;

    GameSystem(String displayName, String gameMasterTitle) {
        this.displayName = displayName;
        this.gameMasterTitle = gameMasterTitle;
    }

    public SheetTemplate sheetTemplate() {
        return SheetTemplates.forSystem(this);
    }
}
