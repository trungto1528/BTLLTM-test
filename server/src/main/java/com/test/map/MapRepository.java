package com.test.map;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.common.map.MapCellData;
import com.test.common.map.MapCellType;
import com.test.common.map.MapData;
import com.test.common.map.MapInfo;

@Component
public class MapRepository {

    private final Map<String, MapData> maps = new LinkedHashMap<>();
    private final Map<String, MapInfo> mapInfos = new LinkedHashMap<>();

    public MapRepository(ObjectMapper objectMapper) {
        loadMap(objectMapper, "map/map01.json");
    }

    public MapData getMap(String mapId) {
        MapData map = maps.get(mapId);

        if (map == null) {
            throw new IllegalArgumentException(
                    "Map not found: " + mapId);
        }

        return map;
    }

    public List<MapInfo> getMapInfos() {
        return Collections.unmodifiableList(
                new ArrayList<>(mapInfos.values()));
    }

    private void loadMap(
            ObjectMapper objectMapper,
            String resourcePath) {

        try {
            ClassPathResource resource =
                    new ClassPathResource(resourcePath);

            try (InputStream inputStream =
                    resource.getInputStream()) {

                MapFile mapFile =
                        objectMapper.readValue(
                                inputStream,
                                MapFile.class);

                MapData map =
                        new MapData(
                                mapFile.width(),
                                mapFile.height(),
                                mapFile.cellSize());

                addCells(
                        map,
                        mapFile.square(),
                        MapCellType.SQUARE);

                addCells(
                        map,
                        mapFile.triangleLeft(),
                        MapCellType.TRIANGLE_LEFT);

                addCells(
                        map,
                        mapFile.triangleRight(),
                        MapCellType.TRIANGLE_RIGHT);

                if (maps.containsKey(mapFile.id())) {
                    throw new IllegalArgumentException(
                            "Duplicate map id: "
                                    + mapFile.id());
                }

                maps.put(
                        mapFile.id(),
                        map);

                mapInfos.put(
                        mapFile.id(),
                        new MapInfo(
                                mapFile.id(),
                                mapFile.name()));
            }

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Failed to load map: "
                            + resourcePath,
                    e);
        }
    }

    private void addCells(
            MapData map,
            List<List<Integer>> coordinates,
            MapCellType type) {

        if (coordinates == null) {
            return;
        }

        for (List<Integer> coordinate : coordinates) {

            if (coordinate == null
                    || coordinate.size() != 2) {

                throw new IllegalArgumentException(
                        "Invalid map coordinate: "
                                + coordinate);
            }

            int gridX = coordinate.get(0);
            int gridY = coordinate.get(1);

            if (!map.isInside(gridX, gridY)) {
                throw new IllegalArgumentException(
                        "Cell is outside map bounds: "
                                + gridX
                                + ", "
                                + gridY);
            }

            if (map.getCell(gridX, gridY) != null) {
                throw new IllegalArgumentException(
                        "Duplicate map cell: "
                                + gridX
                                + ", "
                                + gridY);
            }

            map.addCell(
                    new MapCellData(
                            gridX,
                            gridY,
                            type));
        }
    }

    private record MapFile(
            String id,
            String name,
            int width,
            int height,
            int cellSize,
            List<List<Integer>> square,
            List<List<Integer>> triangleLeft,
            List<List<Integer>> triangleRight) {
    }
}