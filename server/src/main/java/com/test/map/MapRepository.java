package com.test.map;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.common.map.MapCellData;
import com.test.common.map.MapCellType;
import com.test.common.map.MapData;
import com.test.common.map.MapInfo;
import com.test.common.map.MapSpawnData;

@Component
public class MapRepository {

    private static final String MAP_RESOURCE_PATTERN =
            "classpath*:map/*.json";

    private final Map<String, MapData> maps =
            new LinkedHashMap<>();

    private final Map<String, MapInfo> mapInfos =
            new LinkedHashMap<>();

    public MapRepository(
            ObjectMapper objectMapper) {

        loadMaps(objectMapper);
    }

    public MapData getMap(
            String mapId) {

        MapData map =
                maps.get(mapId);

        if (map == null) {

            throw new IllegalArgumentException(
                    "Map not found: "
                            + mapId);
        }

        return map;
    }

    public List<MapInfo> getMapInfos() {

        return Collections.unmodifiableList(
                new ArrayList<>(
                        mapInfos.values()));
    }

    private void loadMaps(
            ObjectMapper objectMapper) {

        PathMatchingResourcePatternResolver resolver =
                new PathMatchingResourcePatternResolver();

        try {

            Resource[] resources =
                    resolver.getResources(
                            MAP_RESOURCE_PATTERN);

            if (resources.length == 0) {

                throw new IllegalStateException(
                        "No map files found: "
                                + MAP_RESOURCE_PATTERN);
            }

            for (Resource resource : resources) {

                loadMap(
                        objectMapper,
                        resource);
            }

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Failed to scan map resources",
                    exception);
        }
    }

    private void loadMap(
            ObjectMapper objectMapper,
            Resource resource) {

        String resourceDescription =
                resource.getDescription();

        try (InputStream inputStream =
                resource.getInputStream()) {

            MapFile mapFile =
                    objectMapper.readValue(
                            inputStream,
                            MapFile.class);

            validateMapFile(
                    mapFile,
                    resourceDescription);

            if (maps.containsKey(
                    mapFile.id())) {

                throw new IllegalArgumentException(
                        "Duplicate map id: "
                                + mapFile.id()
                                + " in "
                                + resourceDescription);
            }

            MapData map =
                    new MapData(
                            mapFile.width(),
                            mapFile.height(),
                            mapFile.cellSize(),
                            mapFile.spawn());

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

            maps.put(
                    mapFile.id(),
                    map);

            mapInfos.put(
                    mapFile.id(),
                    new MapInfo(
                            mapFile.id(),
                            mapFile.name()));

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Failed to load map: "
                            + resourceDescription,
                    exception);
        }
    }

    private void validateMapFile(
            MapFile mapFile,
            String resourceDescription) {

        if (mapFile == null) {

            throw new IllegalArgumentException(
                    "Map file is null: "
                            + resourceDescription);
        }

        if (mapFile.id() == null
                || mapFile.id().isBlank()) {

            throw new IllegalArgumentException(
                    "Map id must not be blank: "
                            + resourceDescription);
        }

        if (mapFile.name() == null
                || mapFile.name().isBlank()) {

            throw new IllegalArgumentException(
                    "Map name must not be blank: "
                            + resourceDescription);
        }

        if (mapFile.width() <= 0
                || mapFile.height() <= 0
                || mapFile.cellSize() <= 0
                || mapFile.width()
                        % mapFile.cellSize() != 0
                || mapFile.height()
                        % mapFile.cellSize() != 0) {

            throw new IllegalArgumentException(
                    "Invalid map dimensions or cell size: "
                            + resourceDescription);
        }

        validateSpawn(
                mapFile,
                resourceDescription);
    }

    private void validateSpawn(
            MapFile mapFile,
            String resourceDescription) {

        if (mapFile.spawn() == null) {

            throw new IllegalArgumentException(
                    "Map spawn must not be null: "
                            + resourceDescription);
        }

        double x =
                mapFile.spawn().getX();

        double y =
                mapFile.spawn().getY();

        if (x < 0
                || x >= mapFile.width()) {

            throw new IllegalArgumentException(
                    "Spawn x is outside map bounds: "
                            + x
                            + " in "
                            + resourceDescription);
        }

        if (y < 0
                || y >= mapFile.height()) {

            throw new IllegalArgumentException(
                    "Spawn y is outside map bounds: "
                            + y
                            + " in "
                            + resourceDescription);
        }
    }

    private void addCells(
            MapData map,
            List<List<Integer>> coordinates,
            MapCellType type) {

        if (coordinates == null) {
            return;
        }

        for (List<Integer> coordinate
                : coordinates) {

            if (coordinate == null
                    || coordinate.size() != 2
                    || coordinate.get(0) == null
                    || coordinate.get(1) == null) {

                throw new IllegalArgumentException(
                        "Invalid map coordinate: "
                                + coordinate);
            }

            int gridX =
                    coordinate.get(0);

            int gridY =
                    coordinate.get(1);

            if (!map.isInside(
                    gridX,
                    gridY)) {

                throw new IllegalArgumentException(
                        "Cell is outside map bounds: "
                                + gridX
                                + ", "
                                + gridY);
            }

            if (map.hasCell(
                    gridX,
                    gridY)) {

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
            MapSpawnData spawn,
            List<List<Integer>> square,
            List<List<Integer>> triangleLeft,
            List<List<Integer>> triangleRight) {
    }
}