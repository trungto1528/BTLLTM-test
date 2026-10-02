package com.test.core;

import java.util.HashMap;
import java.util.Map;

public class PlayerDirectory {

    private final Map<String, String> names =
            new HashMap<>();

    public void setName(
            String playerId,
            String name) {

        if (playerId == null
                || playerId.isBlank()) {

            return;
        }

        if (name == null
                || name.isBlank()) {

            return;
        }

        names.put(
                playerId,
                name);
    }

    public String getName(
            String playerId) {

        if (playerId == null) {
            return null;
        }

        return names.get(playerId);
    }

    public String getNameOrFallback(
            String playerId) {

        String name =
                getName(playerId);

        if (name != null
                && !name.isBlank()) {

            return name;
        }

        if (playerId == null
                || playerId.isBlank()) {

            return "Player";
        }

        if (playerId.length() <= 8) {

            return playerId;
        }

        return playerId.substring(
                0,
                8);
    }

    public void remove(
            String playerId) {

        if (playerId == null) {
            return;
        }

        names.remove(
                playerId);
    }

    public void clear() {

        names.clear();
    }
}