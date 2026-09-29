package com.test.common;

public class InputCommand {

    private final int sequence;
    private final String action;

    /*
     * Giữ field này để tương thích với code client hiện tại.
     *
     * Server không sử dụng giá trị này để tính jump.
     * Jump power authoritative được tính từ game tick.
     */
    private final double jumpPower;

    public InputCommand(
            int sequence,
            String action) {

        this(
                sequence,
                action,
                0);
    }

    public InputCommand(
            int sequence,
            String action,
            double jumpPower) {

        this.sequence = sequence;
        this.action = action;
        this.jumpPower = jumpPower;
    }

    public int getSequence() {
        return sequence;
    }

    public String getAction() {
        return action;
    }

    public double getJumpPower() {
        return jumpPower;
    }
}