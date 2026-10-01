
package com.test.editor;

import com.test.common.map.MapData;

public record MapDocument(
        String id,
        String name,
        MapData mapData) {

    public MapDocument {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "Map id must not be blank");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Map name must not be blank");
        }

        if (mapData == null) {
            throw new IllegalArgumentException(
                    "mapData must not be null");
        }
    }
}