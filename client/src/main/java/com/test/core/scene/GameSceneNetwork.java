package com.test.core.scene;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;

public class GameSceneNetwork {

    private final GameScene scene;

    private long lastServerTick = 0;

    public GameSceneNetwork(
            GameScene scene) {

        this.scene = scene;
    }

    public void handleWorldState(
            String message) {

        String[] parts =
                message.split("\\|");

        if (parts.length < 3) {
            return;
        }

        if (!"WORLD_STATE".equals(parts[0])) {
            return;
        }

        if (!"TICK".equals(parts[1])) {

            System.err.println(
                    "WORLD_STATE missing TICK: "
                            + message);

            return;
        }

        final long serverTick;

        try {

            serverTick =
                    Long.parseLong(
                            parts[2]);

        } catch (NumberFormatException e) {

            System.err.println(
                    "Invalid server tick: "
                            + parts[2]);

            return;
        }

        List<RemoteSnapshot> snapshots =
                new ArrayList<>();

        int index = 3;

        while (index < parts.length) {

            if (!"PLAYER".equals(parts[index])) {

                index++;

                continue;
            }

            if (index + 15 >= parts.length) {

                System.err.println(
                        "Incomplete PLAYER snapshot");

                break;
            }

            try {

                String playerId =
                        parts[index + 1];

                int sequence =
                        Integer.parseInt(
                                parts[index + 2]);

                double x =
                        Double.parseDouble(
                                parts[index + 3]);

                double y =
                        Double.parseDouble(
                                parts[index + 4]);

                double velocityX =
                        Double.parseDouble(
                                parts[index + 5]);

                double velocityY =
                        Double.parseDouble(
                                parts[index + 6]);

                boolean onGround =
                        Boolean.parseBoolean(
                                parts[index + 7]);

                boolean chargingJump =
                        Boolean.parseBoolean(
                                parts[index + 8]);

                boolean chargingUp =
                        Boolean.parseBoolean(
                                parts[index + 9]);

                double maxChargeTimer =
                        Double.parseDouble(
                                parts[index + 10]);

                boolean hasSelectedDirection =
                        Boolean.parseBoolean(
                                parts[index + 11]);

                double jumpPower =
                        Double.parseDouble(
                                parts[index + 12]);

                int facingDirection =
                        Integer.parseInt(
                                parts[index + 13]);

                boolean movingLeft =
                        Boolean.parseBoolean(
                                parts[index + 14]);

                boolean movingRight =
                        Boolean.parseBoolean(
                                parts[index + 15]);

                snapshots.add(
                        new RemoteSnapshot(
                                playerId,
                                sequence,
                                x,
                                y,
                                velocityX,
                                velocityY,
                                onGround,
                                chargingJump,
                                chargingUp,
                                maxChargeTimer,
                                hasSelectedDirection,
                                jumpPower,
                                facingDirection,
                                movingLeft,
                                movingRight));

                index += 16;

            } catch (NumberFormatException e) {

                System.err.println(
                        "Invalid WORLD_STATE player: "
                                + e.getMessage());

                break;
            }
        }

        Platform.runLater(() -> {

            if (serverTick < lastServerTick) {
                return;
            }

            lastServerTick =
                    serverTick;

            for (RemoteSnapshot snapshot
                    : snapshots) {

                if (snapshot.playerId.equals(
                        scene.getLocalPlayerId())) {

                    scene.getSimulation()
                            .reconcileLocalPlayer(
                                    serverTick,
                                    snapshot);

                } else {

                    scene.getRemotePlayers()
                            .updateRemotePlayer(
                                    snapshot);
                }
            }
        });
    }

    public synchronized void handlePlayerState(
            String message) {

        String[] parts =
                message.split("\\|");

        if (parts.length < 7) {
            return;
        }

        try {

            Integer.valueOf(
                    parts[2]);

        } catch (NumberFormatException e) {

            System.err.println(
                    "Invalid PLAYER_STATE: "
                            + message);
        }
    }

    public static class RemoteSnapshot {

        public final String playerId;

        public final int sequence;

        public final double x;
        public final double y;

        public final double velocityX;
        public final double velocityY;

        public final boolean onGround;

        public final boolean chargingJump;
        public final boolean chargingUp;

        public final double maxChargeTimer;

        public final boolean hasSelectedDirection;

        public final double jumpPower;

        public final int facingDirection;

        public final boolean movingLeft;
        public final boolean movingRight;

        public RemoteSnapshot(
                String playerId,
                int sequence,
                double x,
                double y,
                double velocityX,
                double velocityY,
                boolean onGround,
                boolean chargingJump,
                boolean chargingUp,
                double maxChargeTimer,
                boolean hasSelectedDirection,
                double jumpPower,
                int facingDirection,
                boolean movingLeft,
                boolean movingRight) {

            this.playerId =
                    playerId;

            this.sequence =
                    sequence;

            this.x =
                    x;

            this.y =
                    y;

            this.velocityX =
                    velocityX;

            this.velocityY =
                    velocityY;

            this.onGround =
                    onGround;

            this.chargingJump =
                    chargingJump;

            this.chargingUp =
                    chargingUp;

            this.maxChargeTimer =
                    maxChargeTimer;

            this.hasSelectedDirection =
                    hasSelectedDirection;

            this.jumpPower =
                    jumpPower;

            this.facingDirection =
                    facingDirection;

            this.movingLeft =
                    movingLeft;

            this.movingRight =
                    movingRight;
        }
    }
}