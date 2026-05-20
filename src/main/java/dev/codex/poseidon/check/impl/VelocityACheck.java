package dev.codex.poseidon.check.impl;

import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.check.CheckCategory;
import dev.codex.poseidon.check.CheckSeverity;
import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.packet.PacketContext;

public final class VelocityACheck extends Check {
    private final AlertManager alertManager;
    private final long minAgeMs;
    private final long maxAgeMs;
    private final double minHorizontal;
    private final double horizontalRatio;
    private final double verticalRatio;

    public VelocityACheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("VelocityA", plugin, true, 5.0D, CheckCategory.COMBAT, CheckSeverity.HIGH, true,
                "Experimental knockback response validation.");
        this.alertManager = alertManager;
        this.minAgeMs = plugin.getConfig().getLong("checks.VelocityA.min-age-ms", 80L);
        this.maxAgeMs = plugin.getConfig().getLong("checks.VelocityA.max-age-ms", 900L);
        this.minHorizontal = plugin.getConfig().getDouble("checks.VelocityA.min-horizontal", 0.18D);
        this.horizontalRatio = plugin.getConfig().getDouble("checks.VelocityA.min-horizontal-ratio", 0.35D);
        this.verticalRatio = plugin.getConfig().getDouble("checks.VelocityA.min-vertical-ratio", 0.25D);
    }

    @Override
    public void handle(PacketContext context) {
        if (!context.hasPosition()) {
            return;
        }

        PlayerData data = context.getData();
        PlayerData.VelocitySnapshot velocity = data.getLastVelocity();
        if (velocity == null) {
            return;
        }

        long age = context.getTimestamp() - velocity.getTimestamp();
        if (age < minAgeMs || age > maxAgeMs || !data.canVelocityFlag(context.getTimestamp(), 450L)) {
            return;
        }

        if (data.isInLiquid() || data.isInWeb() || data.isOnClimbable() || data.isNearComplexCollision()
                || context.getTimestamp() - data.getLastTeleportTimestamp() < 1500L
                || data.isLatencyCompensated()) {
            return;
        }

        double expectedHorizontal = Math.sqrt(velocity.getX() * velocity.getX() + velocity.getZ() * velocity.getZ());
        if (expectedHorizontal < minHorizontal && Math.abs(velocity.getY()) < 0.18D) {
            return;
        }

        double horizontalRatioTaken = expectedHorizontal <= 0.0D ? 1.0D : data.getLastHorizontalDelta() / expectedHorizontal;
        double verticalRatioTaken = velocity.getY() <= 0.0D ? 1.0D : Math.max(0.0D, data.getLastDeltaY()) / velocity.getY();
        boolean failedHorizontal = expectedHorizontal >= minHorizontal && horizontalRatioTaken < horizontalRatio;
        boolean failedVertical = velocity.getY() > 0.18D && verticalRatioTaken < verticalRatio;

        if (failedHorizontal || failedVertical) {
            double reduction = failedHorizontal ? 1.0D - horizontalRatioTaken : 1.0D - verticalRatioTaken;
            alertManager.flag(context.getPlayer(), data, this, Math.max(0.75D, reduction * 2.0D),
                    "hRatio=" + format(horizontalRatioTaken)
                            + ",vRatio=" + format(verticalRatioTaken)
                            + ",age=" + age + "ms");
        }
    }

    private static String format(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }
}
