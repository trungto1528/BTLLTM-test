package com.test.common;

public class PlayerState {

    private String playerId;

    private double x;
    private double y;

    private double velocityX;
    private double velocityY;

    private boolean onGround;

    /*
     * Đang giữ SPACE để charge jump.
     */
    private boolean chargingJump;

    /*
     * true:
     *     jumpPower đang tăng.
     *
     * false:
     *     đã đạt MAX và đang ở phase giữ/giảm.
     */
    private boolean chargingUp;

    /*
     * Thời gian đã giữ jump ở mức MAX.
     *
     * Đơn vị: giây logical.
     *
     * Được cập nhật bằng GameConfig.TICK_DT,
     * không phụ thuộc FPS.
     */
    private double maxChargeTimer;

    /*
     * Cho biết người chơi đã bấm A/D
     * trong cú jump hiện tại hay chưa.
     */
    private boolean hasSelectedDirection;

    private double jumpPower;

    private int facingDirection;

    public PlayerState() {
    }

    public PlayerState(String playerId) {
        this.playerId = playerId;
    }

    /*
     * =========================
     * PLAYER ID
     * =========================
     */

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    /*
     * =========================
     * POSITION
     * =========================
     */

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    /*
     * =========================
     * VELOCITY
     * =========================
     */

    public double getVelocityX() {
        return velocityX;
    }

    public void setVelocityX(double velocityX) {
        this.velocityX = velocityX;
    }

    public double getVelocityY() {
        return velocityY;
    }

    public void setVelocityY(double velocityY) {
        this.velocityY = velocityY;
    }

    /*
     * =========================
     * GROUND
     * =========================
     */

    public boolean isOnGround() {
        return onGround;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    /*
     * =========================
     * JUMP
     * =========================
     */

    public boolean isChargingJump() {
        return chargingJump;
    }

    public void setChargingJump(boolean chargingJump) {
        this.chargingJump = chargingJump;
    }

    /*
     * =========================
     * JUMP CHARGE PHASE
     * =========================
     */

    public boolean isChargingUp() {
        return chargingUp;
    }

    public void setChargingUp(boolean chargingUp) {
        this.chargingUp = chargingUp;
    }

    /*
     * =========================
     * MAX CHARGE TIMER
     * =========================
     */

    public double getMaxChargeTimer() {
        return maxChargeTimer;
    }

    public void setMaxChargeTimer(
            double maxChargeTimer) {

        this.maxChargeTimer =
                maxChargeTimer;
    }

    /*
     * =========================
     * JUMP DIRECTION
     * =========================
     */

    public boolean hasSelectedDirection() {
        return hasSelectedDirection;
    }

    public void setHasSelectedDirection(
            boolean hasSelectedDirection) {

        this.hasSelectedDirection =
                hasSelectedDirection;
    }

    /*
     * =========================
     * JUMP POWER
     * =========================
     */

    public double getJumpPower() {
        return jumpPower;
    }

    public void setJumpPower(double jumpPower) {
        this.jumpPower = jumpPower;
    }

    /*
     * =========================
     * FACING
     * =========================
     */

    public int getFacingDirection() {
        return facingDirection;
    }

    public void setFacingDirection(
            int facingDirection) {

        this.facingDirection =
                facingDirection;
    }

    /*
     * =========================
     * COPY
     * =========================
     *
     * Copy toàn bộ gameplay state.
     *
     * Quan trọng cho:
     *
     * - server snapshot
     * - reconciliation
     * - prediction replay
     * - backup state
     */

    public void copyFrom(PlayerState other) {

        if (other == null) {
            return;
        }

        this.playerId =
                other.playerId;

        this.x =
                other.x;

        this.y =
                other.y;

        this.velocityX =
                other.velocityX;

        this.velocityY =
                other.velocityY;

        this.onGround =
                other.onGround;

        this.chargingJump =
                other.chargingJump;

        this.chargingUp =
                other.chargingUp;

        this.maxChargeTimer =
                other.maxChargeTimer;

        this.hasSelectedDirection =
                other.hasSelectedDirection;

        this.jumpPower =
                other.jumpPower;

        this.facingDirection =
                other.facingDirection;
    }

    /*
     * =========================
     * COPY
     * =========================
     *
     * Tạo một bản sao độc lập.
     */

    public PlayerState copy() {

        PlayerState copy =
                new PlayerState();

        copy.copyFrom(this);

        return copy;
    }
}