package dev.codex.poseidon.check.impl;

import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.check.CheckCategory;
import dev.codex.poseidon.check.CheckSeverity;
import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.packet.PacketContext;

public final class AimSmoothCheck extends Check {
    private final AlertManager alertManager;
    private final int minSamples;
    private final double maxYawVariance;

    public AimSmoothCheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("AimSmooth", plugin, true, 6.0D, CheckCategory.EXPERIMENTAL, CheckSeverity.MEDIUM, true,
                "Experimental overly-smooth aim variance analysis.");
        this.alertManager = alertManager;
        this.minSamples = plugin.getConfig().getInt("checks.AimSmooth.min-samples", 30);
        this.maxYawVariance = plugin.getConfig().getDouble("checks.AimSmooth.max-yaw-variance", 0.002D);
    }

    @Override
    public void handle(PacketContext context) {
        if (!context.hasRotation() || context.getTimestamp() - context.getData().getLastAttackTimestamp() > 1000L) {
            return;
        }
        PlayerData.RotationStats stats = context.getData().getRotationStats();
        if (stats.getSamples() >= minSamples && stats.getMaxYawDelta() > 1.0D && stats.getYawVariance() < maxYawVariance) {
            alertManager.flag(context.getPlayer(), context.getData(), this, 1.0D,
                    "yawVar=" + format(stats.getYawVariance()) + ",samples=" + stats.getSamples());
        }
    }

    private static String format(double value) {
        return String.format(java.util.Locale.US, "%.4f", value);
    }
}
