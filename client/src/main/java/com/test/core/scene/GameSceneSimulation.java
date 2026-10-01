package com.test.core.scene;

import java.util.ArrayList;
import java.util.List;

import com.test.common.GameConfig;
import com.test.common.InputCommand;
import com.test.common.PlayerState;
import com.test.core.Player;

import javafx.animation.AnimationTimer;

public class GameSceneSimulation {

    private static final int MAX_TICKS_PER_FRAME = 5;

    private final GameScene scene;

    private int inputSequence = 0;

    private long clientTick = 0;

    private double tickAccumulator = 0;

    private double previousLocalX;
    private double previousLocalY;

    private double currentLocalX;
    private double currentLocalY;

    private boolean localRenderInitialized;

    private final List<PendingInput> pendingInputs =
            new ArrayList<>();

    private static class PendingInput {

        private final InputCommand command;

        private final long tick;

        private PendingInput(
                InputCommand command,
                long tick) {

            this.command = command;
            this.tick = tick;
        }
    }

    public GameSceneSimulation(
            GameScene scene) {

        this.scene = scene;

        Player player =
                scene.getPlayer();

        previousLocalX =
                player.getX();

        previousLocalY =
                player.getY();

        currentLocalX =
                player.getX();

        currentLocalY =
                player.getY();

        localRenderInitialized =
                true;
    }

    public long getClientTick() {
        return clientTick;
    }

    public int queueInput(
            String action) {

        int sequence =
                ++inputSequence;

        InputCommand command =
                new InputCommand(
                        sequence,
                        action);

        long inputTick =
                clientTick + 1;

        pendingInputs.add(
                new PendingInput(
                        command,
                        inputTick));

        scene.getNetwork()
                .sendInput(
                        "INPUT|"
                                + sequence
                                + "|"
                                + action);

        return sequence;
    }

    public int sendInput(
            String action) {

        return queueInput(action);
    }

    public int sendJumpRelease() {

        return queueInput(
                "JUMP_RELEASE");
    }

    public void reconcileLocalPlayer(
            long serverTick,
            GameSceneNetwork.RemoteSnapshot snapshot) {

        int lastServerSequence =
                scene.getRemotePlayers()
                        .getLastServerSequence();

        if (snapshot.sequence
                <= lastServerSequence) {

            return;
        }

        long targetClientTick =
                clientTick;

        scene.getRemotePlayers()
                .setLastServerSequence(
                        snapshot.sequence);

        pendingInputs.removeIf(
                input ->
                        input.command.getSequence()
                                <= snapshot.sequence);

        scene.getController()
                .applyServerState(
                        snapshot.x,
                        snapshot.y,
                        snapshot.velocityX,
                        snapshot.velocityY,
                        snapshot.onGround,
                        snapshot.chargingJump,
                        snapshot.chargingUp,
                        snapshot.maxChargeTimer,
                        snapshot.hasSelectedDirection,
                        snapshot.jumpPower,
                        snapshot.facingDirection,
                        snapshot.movingLeft,
                        snapshot.movingRight);

        if (targetClientTick
                <= serverTick) {

            if (clientTick < serverTick) {

                clientTick =
                        serverTick;
            }

            resetLocalRenderInterpolation();

            return;
        }

        for (long tick =
                     serverTick + 1;
             tick <= targetClientTick;
             tick++) {

            for (PendingInput pending
                    : pendingInputs) {

                if (pending.tick == tick) {

                    scene.getController()
                            .applyInput(
                                    pending.command);
                }
            }

            scene.getController()
                    .tick(
                            scene.getMapData());
        }

        resetLocalRenderInterpolation();
    }

    private void resetLocalRenderInterpolation() {

        PlayerState state =
                scene.getController()
                        .getState();

        previousLocalX =
                state.getX();

        previousLocalY =
                state.getY();

        currentLocalX =
                state.getX();

        currentLocalY =
                state.getY();

        localRenderInitialized =
                true;

        scene.getPlayer()
                .setX(
                        state.getX());

        scene.getPlayer()
                .setY(
                        state.getY());

        scene.getPlayer()
                .setOnGround(
                        state.isOnGround());
    }

    private void applyInputsForTick(
            long tick) {

        for (PendingInput pending
                : pendingInputs) {

            if (pending.tick == tick) {

                scene.getController()
                        .applyInput(
                                pending.command);
            }
        }
    }

    private void beginLocalSimulationTick() {

        PlayerState state =
                scene.getController()
                        .getState();

        previousLocalX =
                state.getX();

        previousLocalY =
                state.getY();

        if (!localRenderInitialized) {

            currentLocalX =
                    state.getX();

            currentLocalY =
                    state.getY();

            localRenderInitialized =
                    true;
        }
    }

    private void finishLocalSimulationTick() {

        PlayerState state =
                scene.getController()
                        .getState();

        currentLocalX =
                state.getX();

        currentLocalY =
                state.getY();
    }

    private void syncLocalVisual() {

        if (!localRenderInitialized) {

            PlayerState state =
                    scene.getController()
                            .getState();

            scene.getPlayer()
                    .setX(
                            state.getX());

            scene.getPlayer()
                    .setY(
                            state.getY());

            scene.getPlayer()
                    .setOnGround(
                            state.isOnGround());

            return;
        }

        double alpha =
                tickAccumulator
                        / GameConfig.TICK_DT;

        if (alpha < 0) {
            alpha = 0;
        }

        if (alpha > 1) {
            alpha = 1;
        }

        double renderX =
                previousLocalX
                        + (currentLocalX
                                - previousLocalX)
                                * alpha;

        double renderY =
                previousLocalY
                        + (currentLocalY
                                - previousLocalY)
                                * alpha;

        scene.getPlayer()
                .setX(
                        renderX);

        scene.getPlayer()
                .setY(
                        renderY);

        scene.getPlayer()
                .setOnGround(
                        scene.getController()
                                .getState()
                                .isOnGround());
    }

    public void startLoop() {

        AnimationTimer timer =
                new AnimationTimer() {

                    private long lastTime = 0;

                    @Override
                    public void handle(
                            long now) {

                        if (lastTime == 0) {

                            lastTime = now;

                            return;
                        }

                        double frameDelta =
                                (now - lastTime)
                                        / 1_000_000_000.0;

                        lastTime = now;

                        if (frameDelta > 0.25) {

                            frameDelta = 0.25;
                        }

                        tickAccumulator +=
                                frameDelta;

                        int ticksThisFrame = 0;

                        while (tickAccumulator
                                        >= GameConfig.TICK_DT
                                && ticksThisFrame
                                        < MAX_TICKS_PER_FRAME) {

                            clientTick++;

                            beginLocalSimulationTick();

                            applyInputsForTick(
                                    clientTick);

                            scene.getController()
                                    .tick(
                                            scene.getMapData());

                            finishLocalSimulationTick();

                            tickAccumulator -=
                                    GameConfig.TICK_DT;

                            ticksThisFrame++;
                        }

                        if (ticksThisFrame
                                >= MAX_TICKS_PER_FRAME
                                && tickAccumulator
                                        >= GameConfig.TICK_DT) {

                            tickAccumulator = 0;
                        }

                        syncLocalVisual();

                        scene.getRemotePlayers()
                                .updateRemotePlayers();

                        scene.getCamera()
                                .update();

                        scene.updateJumpBar();
                    }
                };

        timer.start();
    }
}