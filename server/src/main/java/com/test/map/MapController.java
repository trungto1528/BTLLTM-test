package com.test.map;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.test.common.map.MapData;
import com.test.common.map.MapInfo;

@RestController
public class MapController {

    private final MapRepository mapRepository;

    public MapController(MapRepository mapRepository) {
        this.mapRepository = mapRepository;
    }

    @GetMapping("/api/maps")
    public List<MapInfo> getMaps() {
        return mapRepository.getMapInfos();
    }

    @GetMapping("/api/maps/{mapId}")
    public MapData getMap(
            @PathVariable String mapId) {

        return mapRepository.getMap(mapId);
    }
}