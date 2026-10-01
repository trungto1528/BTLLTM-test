package com.test.common;

import com.test.common.map.MapCellData;
import com.test.common.map.MapCellType;
import com.test.common.map.MapData;

public class PhysicsEngine {

    private static final double SLOPE_NORMAL = 0.70710678118;
    private static final double FOOT_PADDING = 2.0;
    private static final double SLOPE_TOLERANCE = 4.0;

    public void tick(
            PlayerState player,
            MapData map,
            boolean movingLeft,
            boolean movingRight) {

        final double deltaTime = GameConfig.TICK_DT;

        /*
         * =========================
         * INPUT HORIZONTAL
         * =========================
         */
        if (player.isOnGround() && !player.isChargingJump()) {
            if (movingLeft && !movingRight) {
                player.setVelocityX(-GameConfig.MOVE_SPEED);
            } else if (movingRight && !movingLeft) {
                player.setVelocityX(GameConfig.MOVE_SPEED);
            } else {
                player.setVelocityX(0);
            }
        }

        double oldX = player.getX();
        double oldY = player.getY();
        double oldBottom = oldY + GameConfig.PLAYER_HEIGHT;

        /*
         * =========================
         * GRAVITY
         * =========================
         */
        if (!player.isChargingJump()) {
            player.setVelocityY(player.getVelocityY() + GameConfig.GRAVITY * deltaTime);
        }

        /*
         * =========================
         * POSITION X & SLOPE STEPPING
         * =========================
         */
        double newX = player.getX() + player.getVelocityX() * deltaTime;
        player.setX(newX);

        resolveHorizontalCollision(player, map, oldX, oldBottom);

        /*
         * =========================
         * POSITION Y
         * =========================
         */
        double newY = player.getY() + player.getVelocityY() * deltaTime;
        player.setY(newY);

        player.setOnGround(false);

        /*
         * =========================
         * VERTICAL COLLISION & SLOPE SLIDE
         * =========================
         */
        resolveVerticalCollision(player, map, oldY, oldBottom);

        /*
         * =========================
         * MAP BOUNDS
         * =========================
         */
        resolveMapBounds(player, map);
    }

    // =====================================================
    // HORIZONTAL COLLISION & SLOPE CLIMBING
    // =====================================================

    private void resolveHorizontalCollision(
            PlayerState player,
            MapData map,
            double oldX,
            double oldBottom) {

        for (MapCellData cell : map.getCells()) {
            if (cell.isEmpty()) {
                continue;
            }

            MapCellType type = cell.getType();
            double x = cell.getGridX() * map.getCellSize();
            double y = cell.getGridY() * map.getCellSize();
            double size = map.getCellSize();

            switch (type) {
                case SQUARE -> resolveSquareHorizontalCollision(player, oldX, x, y, size);
                case TRIANGLE_LEFT -> resolveTriangleLeftHorizontalCollision(player, oldX, oldBottom, x, y, size);
                case TRIANGLE_RIGHT -> resolveTriangleRightHorizontalCollision(player, oldX, oldBottom, x, y, size);
                default -> {}
            }
        }
    }

    private void resolveSquareHorizontalCollision(
            PlayerState player,
            double oldX,
            double x,
            double y,
            double size) {

        boolean verticalOverlap = player.getY() + GameConfig.PLAYER_HEIGHT > y
                && player.getY() < y + size;

        if (!verticalOverlap) {
            return;
        }

        if (player.getVelocityX() > 0) {
            boolean crossed = oldX + GameConfig.PLAYER_WIDTH <= x
                    && player.getX() + GameConfig.PLAYER_WIDTH >= x;

            if (crossed) {
                player.setX(x - GameConfig.PLAYER_WIDTH);
                player.setVelocityX(-player.getVelocityX() * GameConfig.BOUND_RATIO);
            }
        } else if (player.getVelocityX() < 0) {
            boolean crossed = oldX >= x + size
                    && player.getX() <= x + size;

            if (crossed) {
                player.setX(x + size);
                player.setVelocityX(-player.getVelocityX() * GameConfig.BOUND_RATIO);
            }
        }
    }

    private void resolveTriangleLeftHorizontalCollision(
            PlayerState player,
            double oldX,
            double oldBottom,
            double x,
            double y,
            double size) {

        double playerTop = player.getY();
        double playerBottom = player.getY() + GameConfig.PLAYER_HEIGHT;

        if (playerBottom < y || playerTop > y + size) {
            return;
        }

        double triangleRight = x + size;

        // Va chạm vách đứng bên phải
        if (player.getVelocityX() < 0) {
            boolean crossedRightWall = oldX >= triangleRight && player.getX() <= triangleRight;
            if (crossedRightWall) {
                player.setX(triangleRight);
                player.setVelocityX(0);
                return;
            }
        }

        // Leo dốc '/' khi di chuyển sang phải
        if (player.getVelocityX() > 0) {
            double effectiveRight = player.getX() + GameConfig.PLAYER_WIDTH - FOOT_PADDING;
            if (effectiveRight > x && player.getX() < triangleRight) {
                double contactX = Math.max(x, Math.min(triangleRight, effectiveRight));
                double surfaceY = y + size - (contactX - x);
                // Giới hạn trong vùng dốc
                surfaceY = Math.max(y, Math.min(y + size, surfaceY));

                if (playerBottom >= surfaceY - SLOPE_TOLERANCE && playerBottom <= y + size + SLOPE_TOLERANCE) {
                    player.setY(surfaceY - GameConfig.PLAYER_HEIGHT);
                }
            }
        }
    }

    private void resolveTriangleRightHorizontalCollision(
            PlayerState player,
            double oldX,
            double oldBottom,
            double x,
            double y,
            double size) {

        double playerTop = player.getY();
        double playerBottom = player.getY() + GameConfig.PLAYER_HEIGHT;

        if (playerBottom < y || playerTop > y + size) {
            return;
        }

        double triangleLeft = x;

        // Va chạm vách đứng bên trái
        if (player.getVelocityX() > 0) {
            boolean crossedLeftWall = oldX + GameConfig.PLAYER_WIDTH <= triangleLeft
                    && player.getX() + GameConfig.PLAYER_WIDTH >= triangleLeft;
            if (crossedLeftWall) {
                player.setX(triangleLeft - GameConfig.PLAYER_WIDTH);
                player.setVelocityX(0);
                return;
            }
        }

        // Leo dốc '\' khi di chuyển sang trái
        if (player.getVelocityX() < 0) {
            double effectiveLeft = player.getX() + FOOT_PADDING;
            if (effectiveLeft < x + size && player.getX() + GameConfig.PLAYER_WIDTH > x) {
                double contactX = Math.max(x, Math.min(x + size, effectiveLeft));
                double surfaceY = y + (contactX - x);
                // Giới hạn trong vùng dốc
                surfaceY = Math.max(y, Math.min(y + size, surfaceY));

                if (playerBottom >= surfaceY - SLOPE_TOLERANCE && playerBottom <= y + size + SLOPE_TOLERANCE) {
                    player.setY(surfaceY - GameConfig.PLAYER_HEIGHT);
                }
            }
        }
    }

    // =====================================================
    // VERTICAL COLLISION
    // =====================================================

    private void resolveVerticalCollision(
            PlayerState player,
            MapData map,
            double oldY,
            double oldBottom) {

        for (MapCellData cell : map.getCells()) {
            if (cell.isEmpty()) {
                continue;
            }

            double x = cell.getGridX() * map.getCellSize();
            double y = cell.getGridY() * map.getCellSize();
            double size = map.getCellSize();
            MapCellType type = cell.getType();

            switch (type) {
                case SQUARE -> resolveSquareVerticalCollision(player, oldY, x, y, size);
                case TRIANGLE_LEFT -> resolveTriangleLeftCollision(player, x, y, size);
                case TRIANGLE_RIGHT -> resolveTriangleRightCollision(player, x, y, size);
                default -> {}
            }
        }
    }

    private void resolveSquareVerticalCollision(
            PlayerState player,
            double oldY,
            double x,
            double y,
            double size) {

        boolean horizontalOverlap = player.getX() + GameConfig.PLAYER_WIDTH > x
                && player.getX() < x + size;

        if (!horizontalOverlap) {
            return;
        }

        if (player.getVelocityY() >= 0) {
            boolean crossedTop = oldY + GameConfig.PLAYER_HEIGHT <= y
                    && player.getY() + GameConfig.PLAYER_HEIGHT >= y;

            if (crossedTop) {
                player.setY(y - GameConfig.PLAYER_HEIGHT);
                player.setVelocityY(0);
                player.setOnGround(true);
            }
        } else if (player.getVelocityY() < 0) {
            boolean crossedBottom = oldY >= y + size
                    && player.getY() <= y + size;

            if (crossedBottom) {
                player.setY(y + size);
                player.setVelocityY(0);
            }
        }
    }

    // =====================================================
    // TRIANGLE LEFT (Dốc hướng lên bên phải: /)
    // =====================================================

    private void resolveTriangleLeftCollision(
            PlayerState player,
            double x,
            double y,
            double size) {

        double playerLeft = player.getX();
        double playerRight = player.getX() + GameConfig.PLAYER_WIDTH;

        // Kiểm tra phạm vi ngang cơ bản
        if (playerRight <= x || playerLeft >= x + size) {
            return;
        }

        double playerBottom = player.getY() + GameConfig.PLAYER_HEIGHT;

        // Chỉ xử lý nếu chân nằm trong phạm vi ô dốc (mở rộng nhẹ bằng SLOPE_TOLERANCE ở đáy để tránh kẹt)
        if (playerBottom < y - SLOPE_TOLERANCE || playerBottom > y + size + SLOPE_TOLERANCE) {
            return;
        }

        // Tính điểm chân tiếp xúc chuẩn xác
        double contactX = Math.max(x, Math.min(x + size, playerRight - FOOT_PADDING));
        double relativeX = contactX - x;
        double surfaceY = y + size - relativeX;
        surfaceY = Math.max(y, Math.min(y + size, surfaceY));

        // Kiểm tra bám dốc / trượt dốc
        if (Math.abs(playerBottom - surfaceY) <= SLOPE_TOLERANCE || playerBottom > surfaceY) {
            player.setY(surfaceY - GameConfig.PLAYER_HEIGHT);
            applyLeftSlopeVelocity(player);
        } else if (player.getVelocityY() < 0) {
            double triangleBottom = y + size;
            if (player.getY() <= triangleBottom && player.getY() >= y) {
                player.setY(triangleBottom);
                player.setVelocityY(0);
            }
        }
    }

    // =====================================================
    // TRIANGLE RIGHT (Dốc hướng xuống bên phải: \)
    // =====================================================

    private void resolveTriangleRightCollision(
            PlayerState player,
            double x,
            double y,
            double size) {

        double playerLeft = player.getX();
        double playerRight = player.getX() + GameConfig.PLAYER_WIDTH;

        // Kiểm tra phạm vi ngang cơ bản
        if (playerRight <= x || playerLeft >= x + size) {
            return;
        }

        double playerBottom = player.getY() + GameConfig.PLAYER_HEIGHT;

        // Chỉ xử lý nếu chân nằm trong phạm vi ô dốc
        if (playerBottom < y - SLOPE_TOLERANCE || playerBottom > y + size + SLOPE_TOLERANCE) {
            return;
        }

        // Tính điểm chân tiếp xúc chuẩn xác
        double contactX = Math.max(x, Math.min(x + size, playerLeft + FOOT_PADDING));
        double relativeX = contactX - x;
        double surfaceY = y + relativeX;
        surfaceY = Math.max(y, Math.min(y + size, surfaceY));

        // Kiểm tra bám dốc / trượt dốc
        if (Math.abs(playerBottom - surfaceY) <= SLOPE_TOLERANCE || playerBottom > surfaceY) {
            player.setY(surfaceY - GameConfig.PLAYER_HEIGHT);
            applyRightSlopeVelocity(player);
        } else if (player.getVelocityY() < 0) {
            double triangleBottom = y + size;
            if (player.getY() <= triangleBottom && player.getY() >= y) {
                player.setY(triangleBottom);
                player.setVelocityY(0);
            }
        }
    }

    // =====================================================
    // SLOPE SLIDE HELPERS
    // =====================================================

    private void applyLeftSlopeVelocity(PlayerState player) {
        double currentSpeed = Math.hypot(player.getVelocityX(), player.getVelocityY());
        if (currentSpeed < 100.0) {
            currentSpeed = 100.0;
        }
        double slideSpeed = currentSpeed * SLOPE_NORMAL;

        player.setVelocityX(-slideSpeed);
        player.setVelocityY(slideSpeed);
        player.setOnGround(false);
    }

    private void applyRightSlopeVelocity(PlayerState player) {
        double currentSpeed = Math.hypot(player.getVelocityX(), player.getVelocityY());
        if (currentSpeed < 100.0) {
            currentSpeed = 100.0;
        }
        double slideSpeed = currentSpeed * SLOPE_NORMAL;

        player.setVelocityX(slideSpeed);
        player.setVelocityY(slideSpeed);
        player.setOnGround(false);
    }

    // =====================================================
    // MAP BOUNDS
    // =====================================================

    private void resolveMapBounds(PlayerState player, MapData map) {
        double leftWall = map.getCellSize();
        double rightWall = map.getWidth() - map.getCellSize();

        if (player.getX() < leftWall) {
            player.setX(leftWall);
            player.setVelocityX(0);
        }

        if (player.getX() + GameConfig.PLAYER_WIDTH > rightWall) {
            player.setX(rightWall - GameConfig.PLAYER_WIDTH);
            player.setVelocityX(0);
        }

        double floorY = map.getHeight() - map.getCellSize();

        if (player.getY() + GameConfig.PLAYER_HEIGHT > floorY) {
            player.setY(floorY - GameConfig.PLAYER_HEIGHT);
            player.setVelocityY(0);
            player.setOnGround(true);
        }
    }
}