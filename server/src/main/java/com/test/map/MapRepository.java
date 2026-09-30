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
        loadMap(objectMapper, "maps/map01.json");
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

                addFloor(map, mapFile);
                addWalls(map, mapFile);
                addPlatforms(map, mapFile);
                addTriangles(map, mapFile);

                if (maps.containsKey(mapFile.id())) {
                    throw new IllegalArgumentException(
                            "Duplicate map id: " + mapFile.id());
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
                    "Failed to load map: " + resourcePath,
                    e);
        }
    }

    private void addFloor(
            MapData map,
            MapFile mapFile) {

        if (mapFile.floor() == null) {
            return;
        }

        for (int x = 0; x < map.getColumns(); x++) {
            addCell(
                    map,
                    x,
                    mapFile.floor(),
                    MapCellType.SQUARE);
        }
    }

    private void addWalls(
            MapData map,
            MapFile mapFile) {

        if (mapFile.walls() == null) {
            return;
        }

        Walls walls = mapFile.walls();

        for (int y = walls.fromY();
                y <= walls.toY();
                y++) {

            addCell(
                    map,
                    walls.left(),
                    y,
                    MapCellType.SQUARE);

            addCell(
                    map,
                    walls.right(),
                    y,
                    MapCellType.SQUARE);
        }
    }

    private void addPlatforms(
            MapData map,
            MapFile mapFile) {

        if (mapFile.platforms() == null) {
            return;
        }

        for (Platform platform : mapFile.platforms()) {
            for (int x = 0;
                    x < platform.width();
                    x++) {

                addCell(
                        map,
                        platform.x() + x,
                        platform.y(),
                        MapCellType.SQUARE);
            }
        }
    }

    private void addTriangles(
            MapData map,
            MapFile mapFile) {

        if (mapFile.triangles() == null) {
            return;
        }

        for (Triangle triangle : mapFile.triangles()) {
            addCell(
                    map,
                    triangle.x(),
                    triangle.y(),
                    triangle.type());
        }
    }

    private void addCell(
            MapData map,
            int gridX,
            int gridY,
            MapCellType type) {

        if (!map.isInside(gridX, gridY)) {
            return;
        }

        if (map.getCell(gridX, gridY) != null) {
            return;
        }

        map.addCell(
                new MapCellData(
                        gridX,
                        gridY,
                        type));
    }

    private record MapFile(
            String id,
            String name,
            int width,
            int height,
            int cellSize,
            Integer floor,
            Walls walls,
            List<Platform> platforms,
            List<Triangle> triangles) {
    }

    private record Walls(
            int left,
            int right,
            int fromY,
            int toY) {
    }

    private record Platform(
            int x,
            int y,
            int width) {
    }

    private record Triangle(
            int x,
            int y,
            MapCellType type) {
    }
}