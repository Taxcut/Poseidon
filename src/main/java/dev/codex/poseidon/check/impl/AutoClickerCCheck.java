package dev.codex.poseidon.check.impl;

import com.comphenix.protocol.PacketType;
import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.check.CheckCategory;
import dev.codex.poseidon.check.CheckSeverity;
import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.packet.PacketContext;

public final class AutoClickerCCheck extends Check {
    private final AlertManager alertManager;
    private final int minSamples;
    private final double minAverageMs;
    private final double maxDeviationMs;

    public AutoClickerCCheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("AutoClickerC", plugin, true, 4.0D, CheckCategory.COMBAT, CheckSeverity.MEDIUM, true,
                "Experimental low click-variance analysis.");
        this.alertManager = alertManager;
        this.minSamples = plugin.getConfig().getInt("checks.AutoClickerC.min-samples", 24);
        this.minAverageMs = plugin.getConfig().getDouble("checks.AutoClickerC.min-average-ms", 38.0D);
        this.maxDeviationMs = plugin.getConfig().getDouble("checks.AutoClickerC.max-deviation-ms", 4.0D);
    }

    @Override
    public void handle(PacketContext context) {
        if (context.getPacketType() != PacketType.Play.Client.USE_ENTITY || !context.isAttack()) {
            return;
        }
        PlayerData.ClickStats stats = context.getData().getClickStats();
        if (stats.getSamples() >= minSamples
                && stats.getAverage() >= minAverageMs
                && stats.getStandardDeviation() < maxDeviationMs) {
            alertManager.flag(context.getPlayer(), context.getData(), this, 1.0D,
                    "avg=" + format(stats.getAverage()) + ",std=" + format(stats.getStandardDeviation()));
        }
    }

    private static String format(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }
}
