package dev.codex.poseidon.check.impl;

import com.comphenix.protocol.PacketType;
import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.check.CheckCategory;
import dev.codex.poseidon.check.CheckSeverity;
import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.packet.PacketContext;

public final class AutoClickerDCheck extends Check {
    private final AlertManager alertManager;
    private final int minSamples;
    private final double maxVariance;
    private final double minDuplicateRatio;

    public AutoClickerDCheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("AutoClickerD", plugin, true, 5.0D, CheckCategory.COMBAT, CheckSeverity.MEDIUM, true,
                "Experimental combined click pattern/randomization analysis.");
        this.alertManager = alertManager;
        this.minSamples = plugin.getConfig().getInt("checks.AutoClickerD.min-samples", 30);
        this.maxVariance = plugin.getConfig().getDouble("checks.AutoClickerD.max-variance", 35.0D);
        this.minDuplicateRatio = plugin.getConfig().getDouble("checks.AutoClickerD.min-duplicate-ratio", 0.45D);
    }

    @Override
    public void handle(PacketContext context) {
        if (context.getPacketType() != PacketType.Play.Client.USE_ENTITY || !context.isAttack()) {
            return;
        }
        PlayerData.ClickStats stats = context.getData().getClickStats();
        if (stats.getSamples() >= minSamples
                && stats.getVariance() < maxVariance
                && stats.getDuplicateRatio() > minDuplicateRatio) {
            alertManager.flag(context.getPlayer(), context.getData(), this, 1.0D,
                    "var=" + format(stats.getVariance()) + ",dup=" + format(stats.getDuplicateRatio()));
        }
    }

    private static String format(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }
}
