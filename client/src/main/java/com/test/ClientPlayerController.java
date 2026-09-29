package com.test;

import java.util.List;

import com.test.common.GameConfig;
import com.test.common.PhysicsEngine;
import com.test.common.PlatformData;
import com.test.common.PlayerState;

public class ClientPlayerController {

    private final PhysicsEngine physicsEngine;
    private final PlayerState state;

    private boolean movingLeft;
    private boolean movingRight;

    // Chỉ true khi người chơi thực sự bấm A/D
    // trong lúc đang charge jump.
    private boolean hasSelectedDirection;

    public ClientPlayerController(String playerId) {

        physicsEngine = new PhysicsEngine();

        state = new PlayerState(playerId);

        state.setX(180);
        state.setY(1620);

        state.setOnGround(true);
        state.setFacingDirection(1);
    }

    public PlayerState getState() {
        return state;
    }

    public void setMovingLeft(boolean value) {

        movingLeft = value;

        if (value) {
            state.setFacingDirection(-1);

            if (state.isChargingJump()) {
                hasSelectedDirection = true;
            }
        }
    }

    public void setMovingRight(boolean value) {

        movingRight = value;

        if (value) {
            state.setFacingDirection(1);

            if (state.isChargingJump()) {
                hasSelectedDirection = true;
            }
        }
    }

    public void startJump() {

        if (!state.isOnGround()) {
            return;
        }

        if (state.isChargingJump()) {
            return;
        }

        state.setChargingJump(true);
        state.setJumpPower(GameConfig.MIN_JUMP_POWER);

        // Bắt đầu charge => chưa có hướng ngang
        state.setVelocityX(0);
        state.setVelocityY(0);

        hasSelectedDirection = false;
    }

    public void releaseJump(double jumpPower) {

        if (!state.isChargingJump()) {
            return;
        }

        state.setChargingJump(false);
        state.setJumpPower(0);
        state.setOnGround(false);

        // Bay lên
        state.setVelocityY(-jumpPower);

        // Chỉ bay ngang nếu A/D đã được bấm
        // trong cú jump này.
        if (hasSelectedDirection) {

            int direction =
                    state.getFacingDirection();

            state.setVelocityX(
                    direction
                            * jumpPower
                            * GameConfig.HORIZONTAL_JUMP_RATIO);

        } else {

            // SPACE => nhảy thẳng
            state.setVelocityX(0);
        }

        hasSelectedDirection = false;
    }

    public void applyInput(InputCommand input) {

        switch (input.getAction()) {

            case "LEFT_PRESS" ->
                    setMovingLeft(true);

            case "LEFT_RELEASE" ->
                    setMovingLeft(false);

            case "RIGHT_PRESS" ->
                    setMovingRight(true);

            case "RIGHT_RELEASE" ->
                    setMovingRight(false);

            case "JUMP_START" ->
                    startJump();

            case "JUMP_RELEASE" ->
                    releaseJump(input.getJumpPower());

            default ->
                    System.out.println(
                            "Unknown replay input: "
                                    + input.getAction());
        }
    }

    public void applyServerState(
            double x,
            double y,
            double velocityX,
            double velocityY,
            boolean onGround,
            boolean chargingJump,
            double jumpPower,
            int facingDirection) {

        state.setX(x);
        state.setY(y);

        state.setVelocityX(velocityX);
        state.setVelocityY(velocityY);

        state.setOnGround(onGround);

        state.setChargingJump(chargingJump);
        state.setJumpPower(jumpPower);

        state.setFacingDirection(facingDirection);
    }

    public void update(
            double deltaTime,
            List<PlatformData> platforms) {

        physicsEngine.update(
                state,
                deltaTime,
                platforms,
                movingLeft,
                movingRight);
    }
}