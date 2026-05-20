package dev.codex.poseidon.check.impl;

import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.check.CheckCategory;
import dev.codex.poseidon.check.CheckSeverity;
import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.packet.PacketContext;

public final class ModelACheck extends Check {
    private final AlertManager alertManager;
    private final double scoreThreshold;
    private final double minConfidence;
    private final long alertIntervalMs;

    public ModelACheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("ModelA",
                plugin,
                true,
                3.0D,
                CheckCategory.EXPERIMENTAL,
                CheckSeverity.MEDIUM,
                true,
                "Confidence-layer alert from combined behavior score.");
        this.alertManager = alertManager;
        this.scoreThreshold = plugin.getConfig().getDouble("checks.ModelA.score-threshold", 0.78D);
        this.minConfidence = plugin.getConfig().getDouble("checks.ModelA.min-confidence", 0.25D);
        this.alertIntervalMs = plugin.getConfig().getLong("checks.ModelA.alert-interval-ms", 5000L);
    }

    @Override
    public void handle(PacketContext context) {
        PlayerData data = context.getData();
        long now = context.getTimestamp();
        if (now - data.getLastModelScoreUpdate() > 10000L) {
            return;
        }

        if (data.getModelScore() >= scoreThreshold
                && data.getModelConfidence() >= minConfidence
                && data.canModelAlert(now, alertIntervalMs)) {
            double amount = Math.max(1.0D, (data.getModelScore() - scoreThreshold) * 8.0D);
            alertManager.flag(context.getPlayer(), data, this, amount,
                    "score=" + format(data.getModelScore()) + ",confidence=" + format(data.getModelConfidence()));
        }
    }

    private static String format(double value) {
        return String.format(java.util.Locale.US, "%.3f", value);
    }
}
