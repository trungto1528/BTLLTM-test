package com.test.common;

public class PlayerState {

    private String playerId;

    private double x;
    private double y;

    private double velocityX;
    private double velocityY;

    private boolean onGround;
    private boolean chargingJump;

    private double jumpPower;

    private int facingDirection;

    public PlayerState() {
    }

    public PlayerState(String playerId) {
        this.playerId = playerId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

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

    public boolean isOnGround() {
        return onGround;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    public boolean isChargingJump() {
        return chargingJump;
    }

    public void setChargingJump(boolean chargingJump) {
        this.chargingJump = chargingJump;
    }

    public double getJumpPower() {
        return jumpPower;
    }

    public void setJumpPower(double jumpPower) {
        this.jumpPower = jumpPower;
    }

    public int getFacingDirection() {
        return facingDirection;
    }

    public void setFacingDirection(int facingDirection) {
        this.facingDirection = facingDirection;
    }
}