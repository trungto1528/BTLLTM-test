package com.test.core;

import com.test.common.GameConfig;
import com.test.common.InputCommand;
import com.test.common.PhysicsEngine;
import com.test.common.PlayerState;
import com.test.common.map.MapData;

public class ClientPlayerController {

    private final PhysicsEngine physicsEngine;

    private final PlayerState state;

    /*
     * Trạng thái input hiện tại.
     *
     * Đây là input state của client và được thay đổi
     * thông qua InputCommand.
     */
    private boolean movingLeft;
    private boolean movingRight;

    private static final double MAX_HOLD_TIME = 3.0;

    public ClientPlayerController(
            String playerId) {

        physicsEngine =
                new PhysicsEngine();

        state =
                new PlayerState(playerId);

        /*
         * Không hard-code spawn tại đây.
         *
         * Spawn thực tế được lấy từ MapData
         * sau khi client tải map từ server.
         */
        state.setX(0);
        state.setY(0);

        state.setOnGround(false);

        state.setFacingDirection(1);

        state.setChargingJump(false);

        state.setChargingUp(true);

        state.setMaxChargeTimer(0);

        state.setHasSelectedDirection(false);

        state.setJumpPower(0);

        movingLeft = false;
        movingRight = false;
    }

    // =====================================================
    // STATE
    // =====================================================

    public PlayerState getState() {
        return state;
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
            boolean value) {

        movingLeft = value;

        if (value) {

            state.setFacingDirection(-1);

            /*
             * A/D chỉ chọn hướng ngang
             * trong cú jump hiện tại.
             */
            if (state.isChargingJump()) {

                state.setHasSelectedDirection(true);
            }
        }
    }

    public void setMovingRight(
            boolean value) {

        movingRight = value;

        if (value) {

            state.setFacingDirection(1);

            /*
             * A/D chỉ chọn hướng ngang
             * trong cú jump hiện tại.
             */
            if (state.isChargingJump()) {

                state.setHasSelectedDirection(true);
            }
        }
    }

    // =====================================================
    // JUMP CHARGE
    // =====================================================

    public void startJump() {

        if (!state.isOnGround()) {
            return;
        }

        if (state.isChargingJump()) {
            return;
        }

        state.setChargingJump(true);

        state.setChargingUp(true);

        state.setMaxChargeTimer(0);

        state.setJumpPower(
                GameConfig.MIN_JUMP_POWER);

        /*
         * Khi bắt đầu charge:
         *
         * - dừng vận tốc ngang
         * - dừng vận tốc dọc
         */
        state.setVelocityX(0);
        state.setVelocityY(0);

        /*
         * Jump mới chưa chọn hướng ngang.
         */
        state.setHasSelectedDirection(false);
    }

    /**
     * Cập nhật jump charge đúng một logical game tick.
     *
     * Logic:
     *
     * MIN
     *   ↓
     * tăng dần
     *   ↓
     * MAX
     *   ↓
     * giữ MAX 3 giây
     *   ↓
     * giảm dần
     *   ↓
     * MIN
     *   ↓
     * tăng lại
     *
     * Tất cả thời gian đều dựa trên:
     *
     *     GameConfig.TICK_DT
     *
     * Không phụ thuộc FPS render.
     */
    private void tickCharge() {

        if (!state.isChargingJump()) {
            return;
        }

        final double deltaTime =
                GameConfig.TICK_DT;

        double jumpPower =
                state.getJumpPower();

        /*
         * =========================================
         * PHASE 1: TĂNG POWER
         * =========================================
         */
        if (state.isChargingUp()) {

            jumpPower +=
                    GameConfig.CHARGE_SPEED
                            * deltaTime;

            if (jumpPower
                    >= GameConfig.MAX_JUMP_POWER) {

                jumpPower =
                        GameConfig.MAX_JUMP_POWER;

                state.setChargingUp(false);

                state.setMaxChargeTimer(0);
            }
        }

        /*
         * =========================================
         * PHASE 2: GIỮ MAX / GIẢM
         * =========================================
         */
        else {

            double maxChargeTimer =
                    state.getMaxChargeTimer();

            maxChargeTimer +=
                    deltaTime;

            /*
             * Chưa đủ thời gian giữ MAX.
             */
            if (maxChargeTimer
                    < MAX_HOLD_TIME) {

                state.setMaxChargeTimer(
                        maxChargeTimer);

                jumpPower =
                        GameConfig.MAX_JUMP_POWER;

            } else {

                /*
                 * Đã giữ MAX đủ lâu,
                 * bắt đầu giảm power.
                 */
                jumpPower -=
                        GameConfig.CHARGE_SPEED
                                * deltaTime;

                if (jumpPower
                        <= GameConfig.MIN_JUMP_POWER) {

                    jumpPower =
                            GameConfig.MIN_JUMP_POWER;

                    state.setChargingUp(true);

                    state.setMaxChargeTimer(0);

                } else {

                    state.setMaxChargeTimer(
                            maxChargeTimer);
                }
            }
        }

        state.setJumpPower(
                jumpPower);
    }

    // =====================================================
    // JUMP RELEASE
    // =====================================================

    /**
     * Release jump.
     *
     * jumpPower truyền vào chỉ được giữ lại
     * để tương thích với code hiện tại.
     *
     * Prediction không sử dụng giá trị này.
     *
     * Power thực tế lấy từ PlayerState tại
     * đúng tick mà JUMP_RELEASE được apply.
     */
    public void releaseJump(
            double jumpPower) {

        if (!state.isChargingJump()) {
            return;
        }

        /*
         * Luôn lấy jump power hiện tại
         * từ state.
         */
        double actualJumpPower =
                state.getJumpPower();

        /*
         * Clamp.
         */
        if (actualJumpPower
                < GameConfig.MIN_JUMP_POWER) {

            actualJumpPower =
                    GameConfig.MIN_JUMP_POWER;
        }

        if (actualJumpPower
                > GameConfig.MAX_JUMP_POWER) {

            actualJumpPower =
                    GameConfig.MAX_JUMP_POWER;
        }

        state.setChargingJump(false);

        state.setJumpPower(0);

        state.setOnGround(false);

        /*
         * Vận tốc nhảy lên.
         */
        state.setVelocityY(
                -actualJumpPower);

        /*
         * Chỉ bay ngang nếu người chơi
         * đã chọn hướng trong cú jump này.
         */
        if (state.hasSelectedDirection()) {

            int direction =
                    state.getFacingDirection();

            state.setVelocityX(
                    direction
                            * actualJumpPower
                            * GameConfig.HORIZONTAL_JUMP_RATIO);

        } else {

            /*
             * Nhảy thẳng.
             */
            state.setVelocityX(0);
        }

        /*
         * Reset charge state.
         */
        state.setChargingUp(true);

        state.setMaxChargeTimer(0);

        state.setHasSelectedDirection(false);
    }

    // =====================================================
    // INPUT
    // =====================================================

    /**
     * Apply một InputCommand.
     *
     * Được sử dụng cho:
     *
     * 1. Client prediction.
     * 2. Replay sau reconciliation.
     *
     * Input chỉ thay đổi state.
     *
     * Physics được chạy ở tick().
     */
    public void applyInput(
            InputCommand input) {

        if (input == null) {
            return;
        }

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

                startJump();
            }

            case "JUMP_RELEASE" -> {

                /*
                 * Không sử dụng jumpPower từ
                 * InputCommand.
                 *
                 * Lấy power hiện tại từ state.
                 */
                releaseJump(
                        state.getJumpPower());
            }

            default -> {

                System.out.println(
                        "Unknown replay input: "
                                + input.getAction());
            }
        }
    }

    // =====================================================
    // FIXED GAME TICK
    // =====================================================

    /**
     * Chạy đúng một logical game tick.
     *
     * Simulation:
     *
     *     40 TPS
     *
     * Không phụ thuộc FPS render.
     */
    public void tick(
            MapData map) {

        if (map == null) {
            return;
        }

        /*
         * Charge trước physics.
         */
        tickCharge();

        /*
         * Physics sử dụng:
         *
         *     GameConfig.TICK_DT
         *
         * và chính MapData mà server sử dụng.
         */
        physicsEngine.tick(
                state,
                map,
                movingLeft,
                movingRight);
    }

    // =====================================================
    // SERVER RECONCILIATION
    // =====================================================

    /**
     * Restore toàn bộ authoritative state
     * nhận từ server.
     *
     * Bao gồm:
     *
     * 1. Position
     * 2. Velocity
     * 3. Ground state
     * 4. Jump charge state
     * 5. Jump power
     * 6. Facing direction
     * 7. Moving left/right
     *
     * Sau khi restore, GameScene sẽ replay
     * các input chưa được server ACK.
     */
    public void applyServerState(
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

        /*
         * =========================================
         * POSITION
         * =========================================
         */

        state.setX(x);

        state.setY(y);

        /*
         * =========================================
         * VELOCITY
         * =========================================
         */

        state.setVelocityX(
                velocityX);

        state.setVelocityY(
                velocityY);

        /*
         * =========================================
         * GROUND
         * =========================================
         */

        state.setOnGround(
                onGround);

        /*
         * =========================================
         * JUMP CHARGE STATE
         * =========================================
         */

        state.setChargingJump(
                chargingJump);

        state.setChargingUp(
                chargingUp);

        state.setMaxChargeTimer(
                maxChargeTimer);

        state.setHasSelectedDirection(
                hasSelectedDirection);

        state.setJumpPower(
                jumpPower);

        /*
         * =========================================
         * FACING
         * =========================================
         */

        state.setFacingDirection(
                facingDirection);

        /*
         * =========================================
         * MOVEMENT INPUT STATE
         * =========================================
         *
         * Đây là phần quan trọng để reconciliation
         * không làm mất trạng thái A/D.
         */
        this.movingLeft =
                movingLeft;

        this.movingRight =
                movingRight;

        /*
         * =========================================
         * VALIDATION / NORMALIZATION
         * =========================================
         *
         * Nếu server không còn charging:
         * jump charge state phải ở trạng thái
         * sau khi release.
         */
        if (!chargingJump) {

            state.setChargingUp(true);

            state.setMaxChargeTimer(0);

            state.setHasSelectedDirection(false);

            state.setJumpPower(0);
        }

    }
}