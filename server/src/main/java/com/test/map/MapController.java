package com.test.map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.test.common.map.MapData;

@RestController
public class MapController {

    private final GameMap gameMap;

    public MapController(GameMap gameMap) {
        this.gameMap = gameMap;
    }

    @GetMapping("/api/map")
    public MapData getMap() {
        return gameMap.getMap();
    }
}