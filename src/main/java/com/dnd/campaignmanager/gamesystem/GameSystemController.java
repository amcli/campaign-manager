package com.dnd.campaignmanager.gamesystem;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/game-systems")
public class GameSystemController {

    @GetMapping
    public List<GameSystemResponse> listGameSystems() {
        return Arrays.stream(GameSystem.values())
                .map(GameSystemResponse::from)
                .toList();
    }

    @GetMapping("/{system}")
    public GameSystemResponse getGameSystem(@PathVariable GameSystem system) {
        return GameSystemResponse.from(system);
    }
}
