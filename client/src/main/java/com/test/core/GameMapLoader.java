package com.test.core;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.common.map.MapData;

public class GameMapLoader {

    private static final String SERVER_URL =
            "http://localhost:8080";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GameMapLoader() {
        httpClient =
                HttpClient.newHttpClient();

        objectMapper =
                new ObjectMapper();
    }

    public MapData load(
            String mapId)
            throws IOException,
            InterruptedException {

        if (mapId == null
                || mapId.isBlank()) {

            throw new IllegalArgumentException(
                    "mapId must not be blank");
        }

        String url =
                SERVER_URL
                        + "/api/maps/"
                        + mapId;

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {

            throw new IOException(
                    "Failed to load map "
                            + mapId
                            + ": HTTP "
                            + response.statusCode());
        }

        return objectMapper.readValue(
                response.body(),
                MapData.class);
    }
}