package com.test.common;

public final class GameConfig {

    private GameConfig() {
    }

    public static final double MAP_WIDTH = 800;
    public static final double MAP_HEIGHT = 1800;

    public static final double WALL_WIDTH = 20;

    public static final double PLAYER_WIDTH = 30;
    public static final double PLAYER_HEIGHT = 30;

    public static final double GRAVITY = 1500;

    public static final double MIN_JUMP_POWER = 100;
    public static final double MAX_JUMP_POWER = 1300;

    public static final double CHARGE_SPEED = 700;

    public static final double MOVE_SPEED = 250;

    public static final double BOUND_RATIO = 0.35;

    public static final double HORIZONTAL_JUMP_RATIO = 0.65;

    public static final double PHYSICS_HZ = 60;

    public static final double SNAPSHOT_HZ = 20;
}