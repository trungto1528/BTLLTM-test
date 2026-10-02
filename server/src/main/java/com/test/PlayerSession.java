package com.test;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.test.common.GameConfig;
import com.test.common.InputCommand;
import com.test.common.PlayerState;
import com.test.common.map.MapSpawnData;

public class PlayerSession {

    private final PlayerState playerState;

    /*
     * =========================
     * PLAYER IDENTITY
     * =========================
     *
     * Display name là thông tin của
     * connection/session, không thuộc
     * authoritative gameplay state.
     */
    private String displayName;

    private final Queue<InputCommand> inputQueue =
            new ConcurrentLinkedQueue<>();

    /*
     * =========================
     * MOVEMENT
     * =========================
     */

    private boolean movingLeft;
    private boolean movingRight;

    /*
     * =========================
     * INPUT ACK
     * =========================
     *
     * Chỉ tăng khi input thực sự được
     * game tick xử lý.
     */
    private int lastProcessedInput = 0;

    public PlayerSession(String playerId) {

        playerState =
                new PlayerState(playerId);

        displayName = null;

        /*
         * Spawn sẽ được lấy từ MapData sau khi
         * player tạo hoặc join room.
         *
         * Không hard-code tọa độ map tại đây.
         */
        playerState.setX(0);
        playerState.setY(0);

        playerState.setOnGround(false);

        playerState.setFacingDirection(1);

        playerState.setChargingJump(false);

        playerState.setChargingUp(true);

        playerState.setMaxChargeTimer(0);

        playerState.setHasSelectedDirection(false);

        playerState.setJumpPower(0);
    }

    /**
     * Đặt player về spawn point của map.
     *
     * Spawn được lấy từ MapData, không hard-code
     * tọa độ trong PlayerSession.
     */
    public void setSpawn(
            MapSpawnData spawn) {

        if (spawn == null) {
            throw new IllegalArgumentException(
                    "spawn must not be null");
        }

        playerState.setX(
                spawn.getX());

        playerState.setY(
                spawn.getY());

        /*
         * Spawn nằm trên floor của map nên
         * player bắt đầu ở trạng thái đứng trên đất.
         */
        playerState.setOnGround(true);

        /*
         * Reset toàn bộ trạng thái chuyển động
         * khi bắt đầu một room/map mới.
         */
        playerState.setVelocityX(0);
        playerState.setVelocityY(0);

        playerState.setChargingJump(false);
        playerState.setChargingUp(true);
        playerState.setMaxChargeTimer(0);
        playerState.setHasSelectedDirection(false);
        playerState.setJumpPower(0);

        movingLeft = false;
        movingRight = false;
    }

    public PlayerState getPlayerState() {

        return playerState;
    }

    // =====================================================
    // PLAYER IDENTITY
    // =====================================================

    public String getDisplayName() {

        return displayName;
    }

    public void setDisplayName(
            String displayName) {

        this.displayName =
                displayName;
    }

    // =====================================================
    // INPUT QUEUE
    // =====================================================

    /**
     * Đưa input vào queue.
     *
     * Method này có thể được gọi từ WebSocket thread.
     *
     * Không được thay đổi gameplay state ở đây.
     */
    public void queueInput(
            InputCommand input) {

        if (input == null) {
            return;
        }

        /*
         * Không nhận input đã ACK.
         */
        if (input.getSequence()
                <= lastProcessedInput) {

            return;
        }

        inputQueue.offer(input);
    }

    /**
     * Xử lý toàn bộ input đang chờ tại đầu
     * một logical game tick.
     *
     * Chỉ GameServer game thread gọi method này.
     */
    public void processQueuedInputs() {

        InputCommand input;

        while ((input = inputQueue.poll()) != null) {

            int sequence =
                    input.getSequence();

            /*
             * Bỏ qua input cũ hoặc duplicate.
             */
            if (sequence
                    <= lastProcessedInput) {

                continue;
            }

            applyInput(input);

            /*
             * Chỉ ACK sau khi input đã thực sự
             * được áp dụng vào authoritative state.
             */
            lastProcessedInput =
                    sequence;
        }
    }

    /**
     * Apply một input vào authoritative
     * player state.
     *
     * Không chạy physics ở đây.
     *
     * Physics sẽ chạy sau khi toàn bộ input
     * của tick hiện tại được xử lý.
     */
    private void applyInput(
            InputCommand input) {

        switch (input.getAction()) {

            case "LEFT_PRESS" -> {

                setMovingLeft(true);
            }

            case "LEFT_RELEASE" -> {

                setMovingLeft(false);
            }

            case "RIGHT_PRESS" -> {

                setMovingRight(true);
            }

            case "RIGHT_RELEASE" -> {

                setMovingRight(false);
            }

            case "JUMP_START" -> {

                startCharging();
            }

            case "JUMP_RELEASE" -> {

                /*
                 * Server không tin jumpPower
                 * gửi từ client.
                 *
                 * Jump power authoritative nằm
                 * trong PlayerState.
                 */
                releaseJump();
            }

            default -> {

                /*
                 * Input không hợp lệ vẫn được
                 * tiêu thụ để sequence không
                 * bị kẹt.
                 */
                System.out.println(
                        "Unknown input action: "
                                + input.getAction());
            }
        }
    }

    // =====================================================
    // MOVEMENT
    // =====================================================

    public boolean isMovingLeft() {

        return movingLeft;
    }

    public boolean isMovingRight() {

        return movingRight;
    }

    public void setMovingLeft(
            boolean movingLeft) {

        this.movingLeft =
                movingLeft;

        if (movingLeft) {

            playerState.setFacingDirection(-1);

            /*
             * A/D chỉ chọn hướng ngang
             * trong cú jump hiện tại.
             */
            if (playerState.isChargingJump()) {

                playerState.setHasSelectedDirection(
                        true);
            }
        }
    }

    public void setMovingRight(
            boolean movingRight) {

        this.movingRight =
                movingRight;

        if (movingRight) {

            playerState.setFacingDirection(1);

            /*
             * A/D chỉ chọn hướng ngang
             * trong cú jump hiện tại.
             */
            if (playerState.isChargingJump()) {

                playerState.setHasSelectedDirection(
                        true);
            }
        }
    }

    // =====================================================
    // JUMP
    // =====================================================

    public boolean isChargingJump() {

        return playerState.isChargingJump();
    }

    /**
     * Bắt đầu charge jump.
     *
     * Chỉ được gọi từ game thread khi
     * processing input.
     */
    public void startCharging() {

        if (!playerState.isOnGround()) {

            return;
        }

        if (playerState.isChargingJump()) {

            return;
        }

        playerState.setChargingJump(true);

        playerState.setChargingUp(true);

        playerState.setMaxChargeTimer(0);

        playerState.setJumpPower(
                GameConfig.MIN_JUMP_POWER);

        /*
         * Jump mới chưa chọn hướng ngang.
         */
        playerState.setHasSelectedDirection(
                false);

        /*
         * Trong lúc charge không di chuyển.
         */
        playerState.setVelocityX(0);

        playerState.setVelocityY(0);
    }

    /**
     * Cập nhật charge đúng một game tick.
     *
     * Không nhận deltaTime từ bên ngoài.
     *
     * Logic:
     *
     * MIN
     *   ↓
     * tăng
     *   ↓
     * MAX
     *   ↓
     * giữ MAX 3 giây
     *   ↓
     * giảm
     *   ↓
     * MIN
     *   ↓
     * tăng lại
     */
    public void tickCharge() {

        if (!playerState.isChargingJump()) {

            return;
        }

        final double deltaTime =
                GameConfig.TICK_DT;

        double jumpPower =
                playerState.getJumpPower();

        if (playerState.isChargingUp()) {

            jumpPower +=
                    GameConfig.CHARGE_SPEED
                            * deltaTime;

            if (jumpPower
                    >= GameConfig.MAX_JUMP_POWER) {

                jumpPower =
                        GameConfig.MAX_JUMP_POWER;

                playerState.setChargingUp(
                        false);

                playerState.setMaxChargeTimer(
                        0);
            }

        } else {

            double maxChargeTimer =
                    playerState.getMaxChargeTimer();

            maxChargeTimer +=
                    deltaTime;

            playerState.setMaxChargeTimer(
                    maxChargeTimer);

            if (maxChargeTimer
                    >= MAX_HOLD_TIME) {

                jumpPower -=
                        GameConfig.CHARGE_SPEED
                                * deltaTime;

                if (jumpPower
                        <= GameConfig.MIN_JUMP_POWER) {

                    jumpPower =
                            GameConfig.MIN_JUMP_POWER;

                    playerState.setChargingUp(
                            true);

                    playerState.setMaxChargeTimer(
                            0);
                }
            }
        }

        playerState.setJumpPower(
                jumpPower);
    }

    private static final double MAX_HOLD_TIME = 3.0;

    // =====================================================
    // JUMP RELEASE
    // =====================================================

    /**
     * Release jump bằng authoritative
     * jumpPower hiện tại.
     *
     * Không sử dụng jumpPower từ client.
     */
    public void releaseJump() {

        if (!playerState.isChargingJump()) {

            return;
        }

        double jumpPower =
                playerState.getJumpPower();

        /*
         * Clamp để bảo vệ state.
         */
        if (jumpPower
                < GameConfig.MIN_JUMP_POWER) {

            jumpPower =
                    GameConfig.MIN_JUMP_POWER;
        }

        if (jumpPower
                > GameConfig.MAX_JUMP_POWER) {

            jumpPower =
                    GameConfig.MAX_JUMP_POWER;
        }

        playerState.setChargingJump(false);

        playerState.setJumpPower(0);

        playerState.setOnGround(false);

        /*
         * Vertical jump.
         */
        playerState.setVelocityY(
                -jumpPower);

        /*
         * Chỉ có velocity X nếu A/D
         * đã được chọn trong cú jump.
         */
        if (playerState.hasSelectedDirection()) {

            int direction =
                    playerState.getFacingDirection();

            playerState.setVelocityX(
                    direction
                            * jumpPower
                            * GameConfig.HORIZONTAL_JUMP_RATIO);

        } else {

            /*
             * SPACE đơn thuần:
             * nhảy thẳng.
             */
            playerState.setVelocityX(0);
        }

        /*
         * Reset charge state.
         */
        playerState.setChargingUp(true);

        playerState.setMaxChargeTimer(0);

        playerState.setHasSelectedDirection(
                false);
    }

    // =====================================================
    // INPUT ACK
    // =====================================================

    public int getLastProcessedInput() {

        return lastProcessedInput;
    }

    /**
     * Giữ lại method này để tương thích với
     * code cũ nếu nơi khác còn gọi.
     *
     * Tuy nhiên trong kiến trúc mới,
     * ACK chính thức được thực hiện bởi
     * processQueuedInputs().
     */
    public void processInputSequence(
            int sequence) {

        if (sequence > lastProcessedInput) {

            lastProcessedInput =
                    sequence;
        }
    }
}