package com.test;

import java.util.List;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Player extends Rectangle {

    // =========================
    // PHYSICS
    // =========================

    private double velocityX = 0;
    private double velocityY = 0;

    // =========================
    // STATE
    // =========================

    private boolean chargingJump = false;
    private boolean chargingUp = true;

    private boolean onGround = true;

    private boolean movingLeft = false;

    private boolean movingRight = false;

    /*
     * -1 = trái
     * 1 = phải
     */
    private int facingDirection = 1;
    /*
     * Đã chọn hướng trái/phải trong lúc charge hay chưa.
     */
    private boolean hasSelectedDirection = false;

    private double jumpPower = 0;

    // =========================
    // CONSTANTS
    // =========================

    private static final double GRAVITY = 1500;

    private static final double MIN_JUMP_POWER = 100;

    private static final double MAX_JUMP_POWER = 1300;

    private static final double CHARGE_SPEED = 700;

    private static final double MOVE_SPEED = 250;

    private static final double BOUND_RATIO = 0.35;
    /*
     * Tốc độ ngang khi nhảy.
     */
    private static final double HORIZONTAL_JUMP_RATIO = 0.65;

    // =========================
    // MAP
    // =========================

    private static final double MAP_WIDTH = 800;

    private static final double MAP_HEIGHT = 1800;

    private static final double WALL_WIDTH = 20;

    // =========================
    // CONSTRUCTOR
    // =========================

    public Player(
            double x,
            double y) {

        /*
         * Player 30x30
         */
        super(
                30,
                30);

        setFill(
                Color.BLUE);

        setX(x);
        setY(y);
    }

    public void setOnGround(boolean value) {
        onGround = value;
    }

    public boolean isOnGround() {
        return onGround;
    }

    // =====================================================
    // INPUT
    // =====================================================

    public void setMovingLeft(
            boolean value) {

        movingLeft = value;

        /*
         * Khi bấm trái,
         * player quay mặt sang trái.
         */

        if (value) {
            facingDirection = -1;
            hasSelectedDirection = true;
        }
    }

    public void setMovingRight(
            boolean value) {

        movingRight = value;

        /*
         * Khi bấm phải,
         * player quay mặt sang phải.
         */

        if (value) {
            facingDirection = 1;
            hasSelectedDirection = true;
        }
    }

    // =====================================================
    // JUMP
    // =====================================================

    public void startCharging() {

        if (!onGround) {
            return;
        }

        if (chargingJump) {
            return;
        }

        chargingJump = true;

        jumpPower = MIN_JUMP_POWER;

        velocityX = 0;

        hasSelectedDirection = false;

        /*
         * Bắt đầu tăng lực.
         */
        chargingUp = true;
    }

    public void chargeJump(
            double deltaTime) {

        if (!chargingJump) {
            return;
        }

        /*
         * Tăng lực trong 3 giây đầu.
         */
        if (chargingUp) {

            jumpPower += CHARGE_SPEED
                    * deltaTime;

            if (jumpPower >= MAX_JUMP_POWER) {

                jumpPower = MAX_JUMP_POWER;

                /*
                 * Đã đạt tối đa.
                 * Bắt đầu giảm.
                 */
                chargingUp = false;
            }
        }

        /*
         * Sau khi đạt tối đa,
         * lực bắt đầu giảm.
         */
        else {

            jumpPower -= CHARGE_SPEED
                    * deltaTime;

            /*
             * Khi giảm về lực tối thiểu
             * thì quay lại tăng.
             */
            if (jumpPower <= MIN_JUMP_POWER) {

                jumpPower = MIN_JUMP_POWER;

                chargingUp = true;
            }
        }
    }

    public void releaseJump() {

        if (!chargingJump) {
            return;
        }

        chargingJump = false;

        onGround = false;

        /*
         * Nhảy lên.
         *
         * JavaFX:
         * Y âm = đi lên.
         */

        velocityY = -jumpPower;

        /*
         * Nhảy theo hướng
         * player đang quay mặt.
         */

        if (hasSelectedDirection) {

            /*
             * Đã chọn trái/phải
             * -> nhảy chéo.
             */
            velocityX = facingDirection
                    * jumpPower
                    * HORIZONTAL_JUMP_RATIO;

        } else {

            /*
             * Không chọn hướng
             * -> nhảy thẳng đứng.
             */
            velocityX = 0;
        }
        /*
         * Reset charge.
         */

        jumpPower = 0;
    }

    public double getJumpPower() {
        return jumpPower;
    }

    // =====================================================
    // UPDATE
    // =====================================================
    public double getJumpPowerRatio() {

        if (!chargingJump) {
            return 0;
        }

        double chargeRange = MAX_JUMP_POWER
                - MIN_JUMP_POWER;

        double currentCharge = jumpPower
                - MIN_JUMP_POWER;

        return currentCharge / chargeRange;
    }

    public void update(
            double deltaTime,
            List<Platform> platforms) {

        /*
         * =================================================
         * 1. HORIZONTAL MOVEMENT
         * =================================================
         *
         * Chỉ điều khiển ngang khi đang đứng đất.
         *
         * Khi đã nhảy:
         * A/D không thay đổi velocityX.
         */

        if (onGround
                && !chargingJump) {

            /*
             * Đi trái
             */

            if (movingLeft
                    && !movingRight) {

                velocityX = -MOVE_SPEED;
            }

            /*
             * Đi phải
             */

            else if (movingRight
                    && !movingLeft) {

                velocityX = MOVE_SPEED;
            }

            /*
             * Không bấm
             */

            else {

                velocityX = 0;
            }
        }

        /*
         * Lưu vị trí cũ.
         */

        double oldX = getX();

        double oldY = getY();

        /*
         * =================================================
         * 2. GRAVITY
         * =================================================
         */

        velocityY += GRAVITY
                * deltaTime;

        /*
         * =================================================
         * 3. MOVE X
         * =================================================
         */

        double newX = getX()
                + velocityX
                        * deltaTime;

        setX(newX);

        /*
         * =================================================
         * 4. COLLISION LEFT / RIGHT
         * =================================================
         */

        for (Platform platform : platforms) {

            /*
             * Kiểm tra player
             * có overlap theo chiều dọc.
             */

            boolean verticalOverlap = getY()
                    + getHeight() > platform.getY()

                    && getY() < platform.getY()
                            + platform.getHeight();

            if (!verticalOverlap) {
                continue;
            }

            /*
             * ---------------------------------------------
             * Player đi sang phải
             * ---------------------------------------------
             */

            if (velocityX > 0) {

                boolean crossedRightSide = oldX
                        + getWidth() <= platform.getX()

                        && getX()
                                + getWidth() >= platform.getX();

                if (crossedRightSide) {

                    /*
                     * Đưa player ra ngoài platform.
                     */

                    setX(
                            platform.getX()
                                    - getWidth());

                    /*
                     * Bounce ngược lại.
                     *
                     * Mất một nửa tốc độ.
                     */

                    velocityX = -velocityX * BOUND_RATIO;
                }
            }

            /*
             * ---------------------------------------------
             * Player đi sang trái
             * ---------------------------------------------
             */

            else if (velocityX < 0) {

                boolean crossedLeftSide = oldX >= platform.getX()
                        + platform.getWidth()

                        && getX() <= platform.getX()
                                + platform.getWidth();

                if (crossedLeftSide) {

                    /*
                     * Đưa player ra ngoài platform.
                     */

                    setX(
                            platform.getX()
                                    + platform.getWidth());

                    /*
                     * Bounce ngược lại.
                     */

                    velocityX = -velocityX * BOUND_RATIO;
                }
            }
        }

        /*
         * =================================================
         * 5. MOVE Y
         * =================================================
         */

        double newY = getY()
                + velocityY
                        * deltaTime;

        setY(newY);

        /*
         * Mặc định không đứng đất.
         *
         * Nếu collision platform bên dưới
         * thì đặt lại true.
         */

        onGround = false;

        /*
         * =================================================
         * 6. COLLISION TOP / BOTTOM
         * =================================================
         */

        for (Platform platform : platforms) {

            /*
             * Kiểm tra overlap ngang.
             */

            boolean horizontalOverlap = getX()
                    + getWidth() > platform.getX()

                    && getX() < platform.getX()
                            + platform.getWidth();

            if (!horizontalOverlap) {
                continue;
            }

            /*
             * ---------------------------------------------
             * Player đang rơi
             * ---------------------------------------------
             */

            if (velocityY > 0) {

                boolean crossedTop = oldY
                        + getHeight() <= platform.getY()

                        && getY()
                                + getHeight() >= platform.getY();

                if (crossedTop) {

                    /*
                     * Đặt player lên trên platform.
                     */

                    setY(
                            platform.getY()
                                    - getHeight());

                    /*
                     * Dừng rơi.
                     */

                    velocityY = 0;

                    /*
                     * Player đang đứng đất.
                     */

                    onGround = true;
                }
            }

            /*
             * ---------------------------------------------
             * Player đang bay lên
             * ---------------------------------------------
             */

            else if (velocityY < 0) {

                boolean crossedBottom = oldY >= platform.getY()
                        + platform.getHeight()

                        && getY() <= platform.getY()
                                + platform.getHeight();

                if (crossedBottom) {

                    /*
                     * Đụng mặt dưới platform.
                     */

                    setY(
                            platform.getY()
                                    + platform.getHeight());

                    /*
                     * Dừng vận tốc đi lên.
                     */

                    velocityY = 0;
                }
            }
        }

        /*
         * =================================================
         * 7. MAP LEFT WALL
         * =================================================
         *
         * Tường map không bounce.
         */

        if (getX() < WALL_WIDTH) {

            setX(
                    WALL_WIDTH);

            velocityX = 0;
        }

        /*
         * =================================================
         * 8. MAP RIGHT WALL
         * =================================================
         */

        if (getX()
                + getWidth() > MAP_WIDTH
                        - WALL_WIDTH) {

            setX(
                    MAP_WIDTH
                            - WALL_WIDTH
                            - getWidth());

            velocityX = 0;
        }

        /*
         * =================================================
         * 9. MAP FLOOR
         * =================================================
         */

        double floorY = MAP_HEIGHT
                - WALL_WIDTH;

        if (getY()
                + getHeight() > floorY) {

            setY(
                    floorY
                            - getHeight());

            velocityY = 0;

            onGround = true;
        }
    }
}