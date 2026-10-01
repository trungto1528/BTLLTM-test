package com.test.common.map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MapSpawnData {

    private final double x;
    private final double y;

    @JsonCreator
    public MapSpawnData(
            @JsonProperty("x") double x,
            @JsonProperty("y") double y) {

        if (!Double.isFinite(x)) {
            throw new IllegalArgumentException(
                    "spawn x must be finite");
        }

        if (!Double.isFinite(y)) {
            throw new IllegalArgumentException(
                    "spawn y must be finite");
        }

        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}