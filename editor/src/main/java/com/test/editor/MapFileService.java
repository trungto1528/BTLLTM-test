package com.test.editor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.test.common.map.MapData;

public class MapFileService {

    private final ObjectMapper objectMapper;

    public MapFileService() {

        objectMapper =
                new ObjectMapper();

        objectMapper.enable(
                SerializationFeature.INDENT_OUTPUT);
    }

    public void save(
            MapData mapData,
            Path path)
            throws IOException {

        if (mapData == null) {
            throw new IllegalArgumentException(
                    "mapData must not be null");
        }

        if (path == null) {
            throw new IllegalArgumentException(
                    "path must not be null");
        }

        objectMapper.writeValue(
                path.toFile(),
                mapData);
    }

    public MapData load(
            Path path)
            throws IOException {

        if (path == null) {
            throw new IllegalArgumentException(
                    "path must not be null");
        }

        return objectMapper.readValue(
                path.toFile(),
                MapData.class);
    }
}