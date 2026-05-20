package dev.codex.poseidon.check.impl;

import com.comphenix.protocol.PacketType;
import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.check.CheckCategory;
import dev.codex.poseidon.check.CheckSeverity;
import dev.codex.poseidon.packet.PacketContext;

public final class ScaffoldTowerCheck extends Check {
    private final AlertManager alertManager;
    private final double maxUpwardPlaceDelta;

    public ScaffoldTowerCheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("ScaffoldTower", plugin, true, 5.0D, CheckCategory.EXPERIMENTAL, CheckSeverity.MEDIUM, true,
                "Experimental impossible tower acceleration foundation.");
        this.alertManager = alertManager;
        this.maxUpwardPlaceDelta = plugin.getConfig().getDouble("checks.ScaffoldTower.max-upward-delta", 0.72D);
    }

    @Override
    public void handle(PacketContext context) {
        if (context.getPacketType() != PacketType.Play.Client.BLOCK_PLACE) {
            return;
        }
        if (context.getData().getLastDeltaY() > maxUpwardPlaceDelta && context.getData().getPitch() > 70.0F) {
            alertManager.flag(context.getPlayer(), context.getData(), this, 1.0D,
                    "dy=" + format(context.getData().getLastDeltaY()) + ",pitch=" + format(context.getData().getPitch()));
        }
    }

    private static String format(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }
}
