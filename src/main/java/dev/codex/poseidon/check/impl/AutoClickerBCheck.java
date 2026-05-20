package dev.codex.poseidon.check.impl;

import com.comphenix.protocol.PacketType;
import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.check.CheckCategory;
import dev.codex.poseidon.check.CheckSeverity;
import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.packet.PacketContext;

public final class AutoClickerBCheck extends Check {
    private final AlertManager alertManager;
    private final int minSamples;
    private final double maxDuplicateRatio;

    public AutoClickerBCheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("AutoClickerB", plugin, true, 4.0D, CheckCategory.COMBAT, CheckSeverity.MEDIUM, true,
                "Experimental duplicate click-delay analysis.");
        this.alertManager = alertManager;
        this.minSamples = plugin.getConfig().getInt("checks.AutoClickerB.min-samples", 20);
        this.maxDuplicateRatio = plugin.getConfig().getDouble("checks.AutoClickerB.max-duplicate-ratio", 0.72D);
    }

    @Override
    public void handle(PacketContext context) {
        if (context.getPacketType() != PacketType.Play.Client.USE_ENTITY || !context.isAttack()) {
            return;
        }
        PlayerData.ClickStats stats = context.getData().getClickStats();
        if (stats.getSamples() >= minSamples && stats.getDuplicateRatio() > maxDuplicateRatio) {
            alertManager.flag(context.getPlayer(), context.getData(), this, 1.0D,
                    "dup=" + format(stats.getDuplicateRatio()) + ",samples=" + stats.getSamples());
        }
    }

    private static String format(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }
}
