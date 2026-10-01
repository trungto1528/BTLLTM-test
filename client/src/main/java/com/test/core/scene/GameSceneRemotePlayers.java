package com.test.core.scene;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.test.common.GameConfig;
import com.test.common.PlayerState;
import com.test.core.Player;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

public class GameSceneRemotePlayers {

    private final GameScene scene;

    private final Map<String, Player> remotePlayers =
            new HashMap<>();

    private final Map<String, RemoteState> remoteStates =
            new HashMap<>();

    private final Set<String> disconnectedPlayerIds =
            new HashSet<>();

    private final Map<String, Group> disconnectedIndicators =
            new HashMap<>();

    private int lastServerSequence = 0;

    private static class RemoteState {

        private final PlayerState state;

        private double previousX;
        private double previousY;

        private double targetX;
        private double targetY;

        private boolean targetOnGround;

        private boolean initialized;

        private long snapshotTimeNanos;

        private RemoteState(
                String playerId) {

            state =
                    new PlayerState(
                            playerId);
        }
    }

    public GameSceneRemotePlayers(
            GameScene scene) {

        this.scene = scene;
    }

    public int getLastServerSequence() {
        return lastServerSequence;
    }

    public void setLastServerSequence(
            int sequence) {

        lastServerSequence =
                sequence;
    }

    public void updateRemotePlayer(
            GameSceneNetwork.RemoteSnapshot snapshot) {

        String playerId =
                snapshot.playerId;

        if (playerId.equals(
                scene.getLocalPlayerId())) {

            return;
        }

        Player remote =
                remotePlayers.get(
                        playerId);

        if (remote == null) {

            remote =
                    new Player(
                            snapshot.x,
                            snapshot.y);

            remotePlayers.put(
                    playerId,
                    remote);

            scene.getWorld()
                    .getChildren()
                    .add(remote);
        }

        RemoteState remoteState =
                remoteStates.computeIfAbsent(
                        playerId,
                        RemoteState::new);

        if (!remoteState.initialized) {

            remoteState.previousX =
                    snapshot.x;

            remoteState.previousY =
                    snapshot.y;

            remoteState.targetX =
                    snapshot.x;

            remoteState.targetY =
                    snapshot.y;


            remoteState.targetOnGround =
                    snapshot.onGround;

            remoteState.initialized =
                    true;

        } else {

            remoteState.previousX =
                    remoteState.targetX;

            remoteState.previousY =
                    remoteState.targetY;


            remoteState.targetX =
                    snapshot.x;

            remoteState.targetY =
                    snapshot.y;

            remoteState.targetOnGround =
                    snapshot.onGround;
        }

        remoteState.state.setX(
                snapshot.x);

        remoteState.state.setY(
                snapshot.y);

        remoteState.state.setVelocityX(
                snapshot.velocityX);

        remoteState.state.setVelocityY(
                snapshot.velocityY);

        remoteState.state.setOnGround(
                snapshot.onGround);

        remoteState.state.setChargingJump(
                snapshot.chargingJump);

        remoteState.state.setChargingUp(
                snapshot.chargingUp);

        remoteState.state.setMaxChargeTimer(
                snapshot.maxChargeTimer);

        remoteState.state.setHasSelectedDirection(
                snapshot.hasSelectedDirection);

        remoteState.state.setJumpPower(
                snapshot.jumpPower);

        remoteState.state.setFacingDirection(
                snapshot.facingDirection);

        remoteState.snapshotTimeNanos =
                System.nanoTime();

        if (disconnectedPlayerIds.contains(
                playerId)) {

            showDisconnectedIndicator(
                    playerId);
        }
    }

    public void updateRemotePlayers() {

        long now =
                System.nanoTime();

        final double snapshotIntervalNanos =
                GameConfig.TICK_NANOS
                        * GameConfig.SNAPSHOT_INTERVAL;

        for (Map.Entry<String, Player> entry
                : remotePlayers.entrySet()) {

            String playerId =
                    entry.getKey();

            Player remote =
                    entry.getValue();

            RemoteState remoteState =
                    remoteStates.get(
                            playerId);

            if (remoteState == null
                    || !remoteState.initialized) {

                continue;
            }

            double alpha =
                    (now
                            - remoteState.snapshotTimeNanos)
                            / snapshotIntervalNanos;

            if (alpha < 0) {
                alpha = 0;
            }

            if (alpha > 1) {
                alpha = 1;
            }

            double x =
                    remoteState.previousX
                            + (remoteState.targetX
                                    - remoteState.previousX)
                                    * alpha;

            double y =
                    remoteState.previousY
                            + (remoteState.targetY
                                    - remoteState.previousY)
                                    * alpha;

            remote.setX(x);
            remote.setY(y);

            remote.setOnGround(
                    remoteState.targetOnGround);

            updateDisconnectedIndicatorPosition(
                    playerId);
        }
    }

    public void handlePlayerLeft(
            String message) {

        String[] parts =
                message.split("\\|", -1);

        /*
         * PLAYER_LEFT|roomId|playerId|wasHost|newHostId
         */
        if (parts.length < 3) {
            return;
        }

        String playerId =
                parts[2];

        if (playerId.isBlank()
                || playerId.equals(
                        scene.getLocalPlayerId())) {

            return;
        }

        disconnectedPlayerIds.add(
                playerId);

        showDisconnectedIndicator(
                playerId);
    }

    private void showDisconnectedIndicator(
            String playerId) {

        if (disconnectedIndicators.containsKey(
                playerId)) {

            updateDisconnectedIndicatorPosition(
                    playerId);

            return;
        }

        Player remote =
                remotePlayers.get(
                        playerId);

        if (remote == null) {
            return;
        }

        Group indicator =
                new Group();

        Rectangle bar1 =
                new Rectangle(
                        0,
                        7,
                        3,
                        5);

        Rectangle bar2 =
                new Rectangle(
                        5,
                        4,
                        3,
                        8);

        Rectangle bar3 =
                new Rectangle(
                        10,
                        0,
                        3,
                        12);

        bar1.setFill(
                Color.RED);

        bar2.setFill(
                Color.RED);

        bar3.setFill(
                Color.RED);

        Line slash =
                new Line(
                        0,
                        0,
                        14,
                        14);

        slash.setStroke(
                Color.RED);

        slash.setStrokeWidth(
                2.5);

        indicator.getChildren().addAll(
                bar1,
                bar2,
                bar3,
                slash);

        disconnectedIndicators.put(
                playerId,
                indicator);

        scene.getWorld()
                .getChildren()
                .add(indicator);

        updateDisconnectedIndicatorPosition(
                playerId);
    }

    private void updateDisconnectedIndicatorPosition(
            String playerId) {

        Player remote =
                remotePlayers.get(
                        playerId);

        Group indicator =
                disconnectedIndicators.get(
                        playerId);

        if (remote == null
                || indicator == null) {

            return;
        }

        indicator.setLayoutX(
                remote.getX()
                        + remote.getWidth() / 2.0
                        - 7);

        indicator.setLayoutY(
                remote.getY()
                        - 20);
    }
}