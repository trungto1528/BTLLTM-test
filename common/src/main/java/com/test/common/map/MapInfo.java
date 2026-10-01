package com.test.common.map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MapInfo {

    private final String id;
    private final String name;

    @JsonCreator
    public MapInfo(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name) {

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "id must not be blank");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "name must not be blank");
        }

        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}