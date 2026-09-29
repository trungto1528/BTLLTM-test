package com.test;

import com.test.common.GameConfig;
import com.test.common.PlayerState;

public class PlayerSession {

    private final PlayerState playerState;

    private boolean movingLeft;
    private boolean movingRight;

    // Chỉ true khi người chơi thực sự bấm A/D
    // trong cú jump hiện tại.
    private boolean hasSelectedDirection;

    private boolean chargingJump;
    private boolean chargingUp;

    private double jumpPower;

    private static final double MAX_HOLD_TIME = 3.0;
    private double maxChargeTimer = 0;

    private int lastProcessedInput = 0;

    public PlayerSession(String playerId) {

        playerState = new PlayerState(playerId);

        playerState.setX(180);
        playerState.setY(1620);

        playerState.setOnGround(true);
        playerState.setFacingDirection(1);
    }

    public PlayerState getPlayerState() {
        return playerState;
    }

    // =========================
    // MOVEMENT
    // =========================

    public boolean isMovingLeft() {
        return movingLeft;
    }

    public boolean isMovingRight() {
        return movingRight;
    }

    public void setMovingLeft(boolean movingLeft) {

        this.movingLeft = movingLeft;

        if (movingLeft) {

            playerState.setFacingDirection(-1);

            // Chỉ chọn hướng nếu đang charge jump
            if (chargingJump) {
                hasSelectedDirection = true;
            }
        }
    }

    public void setMovingRight(boolean movingRight) {

        this.movingRight = movingRight;

        if (movingRight) {

            playerState.setFacingDirection(1);

            // Chỉ chọn hướng nếu đang charge jump
            if (chargingJump) {
                hasSelectedDirection = true;
            }
        }
    }

    // =========================
    // JUMP CHARGE
    // =========================

    public boolean isChargingJump() {
        return chargingJump;
    }

    public void startCharging() {

        if (!playerState.isOnGround()) {
            return;
        }

        if (chargingJump) {
            return;
        }

        chargingJump = true;

        chargingUp = true;

        jumpPower = GameConfig.MIN_JUMP_POWER;

        maxChargeTimer = 0;

        // Cú jump mới => chưa chọn hướng ngang
        hasSelectedDirection = false;

        playerState.setChargingJump(true);

        playerState.setJumpPower(jumpPower);

        // Khi charge không di chuyển ngang
        playerState.setVelocityX(0);

        playerState.setVelocityY(0);
    }

    public void updateCharge(double deltaTime) {

        if (!chargingJump) {
            return;
        }

        if (chargingUp) {

            jumpPower +=
                    GameConfig.CHARGE_SPEED
                            * deltaTime;

            if (jumpPower
                    >= GameConfig.MAX_JUMP_POWER) {

                jumpPower =
                        GameConfig.MAX_JUMP_POWER;

                chargingUp = false;

                maxChargeTimer = 0;
            }

        } else {

            maxChargeTimer += deltaTime;

            if (maxChargeTimer >= MAX_HOLD_TIME) {

                jumpPower -=
                        GameConfig.CHARGE_SPEED
                                * deltaTime;

                if (jumpPower
                        <= GameConfig.MIN_JUMP_POWER) {

                    jumpPower =
                            GameConfig.MIN_JUMP_POWER;

                    chargingUp = true;

                    maxChargeTimer = 0;
                }
            }
        }

        playerState.setJumpPower(jumpPower);
    }

    // =========================
    // JUMP RELEASE
    // =========================

    public void releaseJump() {

        if (!chargingJump) {
            return;
        }

        chargingJump = false;

        playerState.setChargingJump(false);

        playerState.setJumpPower(0);

        playerState.setOnGround(false);

        // Bay lên
        playerState.setVelocityY(-jumpPower);

        // Chỉ bay ngang nếu A/D
        // đã được bấm trong cú jump này.
        if (hasSelectedDirection) {

            int direction =
                    playerState.getFacingDirection();

            playerState.setVelocityX(
                    direction
                            * jumpPower
                            * GameConfig.HORIZONTAL_JUMP_RATIO
            );

        } else {

            // SPACE đơn thuần => nhảy thẳng
            playerState.setVelocityX(0);
        }

        jumpPower = 0;

        maxChargeTimer = 0;

        chargingUp = true;

        hasSelectedDirection = false;
    }

    // =========================
    // INPUT SEQUENCE
    // =========================

    public int getLastProcessedInput() {
        return lastProcessedInput;
    }

    public void processInputSequence(int sequence) {

        // Không cho ACK quay ngược
        if (sequence > lastProcessedInput) {
            lastProcessedInput = sequence;
        }
    }
}