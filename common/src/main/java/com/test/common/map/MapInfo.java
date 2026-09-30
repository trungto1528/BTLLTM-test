package com.test.common.map;

public class MapInfo {

    private final String id;
    private final String name;

    public MapInfo(
            String id,
            String name) {

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