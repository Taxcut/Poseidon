package dev.codex.poseidon.simulation;

import dev.codex.poseidon.data.PlayerData;

public final class MovementSimulator {
    private final int maxTicks;
    private final double horizontalBuffer;
    private final double verticalBuffer;
    private final long velocityAgeMs;

    public MovementSimulator(int maxTicks, double horizontalBuffer, double verticalBuffer, long velocityAgeMs) {
        this.maxTicks = Math.max(1, maxTicks);
        this.horizontalBuffer = Math.max(0.0D, horizontalBuffer);
        this.verticalBuffer = Math.max(0.0D, verticalBuffer);
        this.velocityAgeMs = Math.max(0L, velocityAgeMs);
    }

    public MovementEnvelope predict(PlayerData data, long timestamp) {
        int ticks = elapsedTicks(data.getLastPositionTimestamp(), timestamp);
        double baseHorizontal = data.wasOnGround() || data.isOnGround() ? 0.42D : 0.36D;
        double maxUpward = (data.wasOnGround() ? 0.62D : 0.42D) * ticks + verticalBuffer;
        double maxDownward = 0.98D * ticks + verticalBuffer;

        if (data.isSprinting()) {
            baseHorizontal *= 1.22D;
        }

        if (data.isSneaking()) {
            baseHorizontal *= 0.55D;
        }

        if (data.isInWeb()) {
            baseHorizontal = Math.max(baseHorizontal, 0.18D);
            maxUpward = Math.max(maxUpward, 0.25D * ticks + verticalBuffer);
            maxDownward = Math.max(maxDownward, 0.35D * ticks + verticalBuffer);
        }

        if (data.isInLiquid()) {
            baseHorizontal = Math.max(baseHorizontal, 0.30D);
            maxUpward = Math.max(maxUpward, 0.35D * ticks + verticalBuffer);
            maxDownward = Math.max(maxDownward, 0.55D * ticks + verticalBuffer);
        }

        if (data.isOnIce()) {
            baseHorizontal *= 1.45D;
        }

        if (data.isOnSlime()) {
            maxUpward += 0.45D;
            maxDownward += 0.45D;
        }

        if (data.isNearComplexCollision()) {
            baseHorizontal += 0.16D;
            maxUpward += 0.20D;
            maxDownward += 0.20D;
        }

        if (data.isOnSoulSand()) {
            baseHorizontal *= 0.75D;
        }

        if (data.isOnClimbable()) {
            baseHorizontal = Math.max(baseHorizontal, 0.32D);
            maxUpward = Math.max(maxUpward, 0.28D * ticks + verticalBuffer);
            maxDownward = Math.max(maxDownward, 0.25D * ticks + verticalBuffer);
        }

        int speedAmplifier = data.getSpeedAmplifier();
        if (speedAmplifier >= 0) {
            baseHorizontal *= 1.0D + 0.20D * (speedAmplifier + 1);
        }

        double maxHorizontal = baseHorizontal * ticks + horizontalBuffer;

        int jumpAmplifier = data.getJumpAmplifier();
        if (jumpAmplifier >= 0) {
            maxUpward += 0.10D * (jumpAmplifier + 1);
        }

        PlayerData.VelocitySnapshot velocity = data.getLastVelocity();
        if (velocity != null && timestamp - velocity.getTimestamp() <= velocityAgeMs) {
            double velocityHorizontal = Math.sqrt(velocity.getX() * velocity.getX() + velocity.getZ() * velocity.getZ());
            maxHorizontal += velocityHorizontal * 1.35D;
            if (velocity.getY() > 0.0D) {
                maxUpward += velocity.getY() * 1.35D;
            } else {
                maxDownward += Math.abs(velocity.getY()) * 1.35D;
            }
        }

        return new MovementEnvelope(maxHorizontal, maxUpward, maxDownward);
    }

    private int elapsedTicks(long lastTimestamp, long timestamp) {
        if (lastTimestamp <= 0L || timestamp <= lastTimestamp) {
            return 1;
        }
        long elapsed = timestamp - lastTimestamp;
        int ticks = (int) Math.ceil(elapsed / 50.0D);
        return Math.max(1, Math.min(maxTicks, ticks));
    }
}
