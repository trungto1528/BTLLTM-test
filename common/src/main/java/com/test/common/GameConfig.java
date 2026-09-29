package com.test.common;

public final class GameConfig {

    private GameConfig() {
    }

    // =========================
    // MAP
    // =========================

    public static final double MAP_WIDTH = 800;
    public static final double MAP_HEIGHT = 1800;

    public static final double WALL_WIDTH = 20;


    // =========================
    // PLAYER
    // =========================

    public static final double PLAYER_WIDTH = 30;
    public static final double PLAYER_HEIGHT = 30;


    // =========================
    // PHYSICS
    // =========================

    public static final double GRAVITY = 1500;

    public static final double MOVE_SPEED = 250;


    // =========================
    // JUMP
    // =========================

    public static final double MIN_JUMP_POWER = 100;
    public static final double MAX_JUMP_POWER = 1300;

    public static final double CHARGE_SPEED = 700;

    public static final double BOUND_RATIO = 0.35;

    public static final double HORIZONTAL_JUMP_RATIO = 0.65;


    // =========================
    // GAME TICK
    // =========================

    /**
     * Toàn bộ simulation/gameplay chạy cố định 60 tick mỗi giây.
     *
     * 1 tick = 1 / 60 giây.
     */
    public static final int TICK_RATE = 40;

    /**
     * Thời gian của một game tick.
     */
    public static final double TICK_DT =
            1.0 / TICK_RATE;

    /**
     * Thời gian một tick tính bằng nanosecond.
     */
    public static final long TICK_NANOS =
            1_000_000_000L / TICK_RATE;


    // =========================
    // NETWORK
    // =========================

    /**
     * Tần suất gửi snapshot từ server
     * xuống client.
     *
     * Game vẫn chạy 60 tick/s.
     * Network snapshot chỉ gửi 20 lần/s.
     */
    public static final int SNAPSHOT_RATE = 20;

    /**
     * Cứ bao nhiêu game tick thì gửi
     * một snapshot.
     *
     * 60 / 20 = 3
     */
    public static final int SNAPSHOT_INTERVAL =
            TICK_RATE / SNAPSHOT_RATE;
}