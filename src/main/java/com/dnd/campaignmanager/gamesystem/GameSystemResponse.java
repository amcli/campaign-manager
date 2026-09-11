package com.dnd.campaignmanager.gamesystem;

import java.util.List;

public record GameSystemResponse(
        GameSystem code,
        String name,
        String gameMasterTitle,
        List<SheetSection> sections
) {
    public static GameSystemResponse from(GameSystem system) {
        return new GameSystemResponse(
                system,
                system.getDisplayName(),
                system.getGameMasterTitle(),
                system.sheetTemplate().sections());
    }
}
