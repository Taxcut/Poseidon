package dev.codex.poseidon.check.impl;

import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.check.CheckCategory;
import dev.codex.poseidon.check.CheckSeverity;
import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.packet.PacketContext;

public final class AimConsistencyCheck extends Check {
    private final AlertManager alertManager;
    private final int minSamples;
    private final double maxCombinedVariance;

    public AimConsistencyCheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("AimConsistency", plugin, true, 6.0D, CheckCategory.EXPERIMENTAL, CheckSeverity.MEDIUM, true,
                "Experimental low-entropy combat rotation consistency analysis.");
        this.alertManager = alertManager;
        this.minSamples = plugin.getConfig().getInt("checks.AimConsistency.min-samples", 35);
        this.maxCombinedVariance = plugin.getConfig().getDouble("checks.AimConsistency.max-combined-variance", 0.025D);
    }

    @Override
    public void handle(PacketContext context) {
        if (!context.hasRotation() || context.getTimestamp() - context.getData().getLastAttackTimestamp() > 1000L) {
            return;
        }
        PlayerData.RotationStats stats = context.getData().getRotationStats();
        double combined = stats.getYawVariance() + stats.getPitchVariance();
        if (stats.getSamples() >= minSamples && combined < maxCombinedVariance && stats.getMaxYawDelta() > 2.0D) {
            alertManager.flag(context.getPlayer(), context.getData(), this, 1.0D,
                    "combinedVar=" + format(combined) + ",samples=" + stats.getSamples());
        }
    }

    private static String format(double value) {
        return String.format(java.util.Locale.US, "%.4f", value);
    }
}
