package dev.codex.poseidon.check.impl;

import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.check.CheckCategory;
import dev.codex.poseidon.check.CheckSeverity;
import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.packet.PacketContext;

public final class AimDivisorCheck extends Check {
    private final AlertManager alertManager;
    private final int minSamples;
    private final double minDuplicateRatio;

    public AimDivisorCheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("AimDivisor", plugin, true, 6.0D, CheckCategory.EXPERIMENTAL, CheckSeverity.MEDIUM, true,
                "Experimental duplicate rotation divisor analysis.");
        this.alertManager = alertManager;
        this.minSamples = plugin.getConfig().getInt("checks.AimDivisor.min-samples", 28);
        this.minDuplicateRatio = plugin.getConfig().getDouble("checks.AimDivisor.min-duplicate-ratio", 0.62D);
    }

    @Override
    public void handle(PacketContext context) {
        if (!context.hasRotation() || context.getTimestamp() - context.getData().getLastAttackTimestamp() > 1000L) {
            return;
        }
        PlayerData.RotationStats stats = context.getData().getRotationStats();
        if (stats.getSamples() >= minSamples && stats.getDuplicateRatio() > minDuplicateRatio) {
            alertManager.flag(context.getPlayer(), context.getData(), this, 1.0D,
                    "dup=" + format(stats.getDuplicateRatio()) + ",samples=" + stats.getSamples());
        }
    }

    private static String format(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }
}
