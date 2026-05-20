package dev.codex.poseidon.check.impl;

import com.comphenix.protocol.PacketType;
import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.check.CheckCategory;
import dev.codex.poseidon.check.CheckSeverity;
import dev.codex.poseidon.packet.PacketContext;

public final class ScaffoldRotationCheck extends Check {
    private final AlertManager alertManager;
    private final double minPitch;
    private final double minMove;

    public ScaffoldRotationCheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("ScaffoldRotation", plugin, true, 5.0D, CheckCategory.EXPERIMENTAL, CheckSeverity.MEDIUM, true,
                "Experimental bridge placement rotation alignment check.");
        this.alertManager = alertManager;
        this.minPitch = plugin.getConfig().getDouble("checks.ScaffoldRotation.min-pitch", 55.0D);
        this.minMove = plugin.getConfig().getDouble("checks.ScaffoldRotation.min-move", 0.24D);
    }

    @Override
    public void handle(PacketContext context) {
        if (context.getPacketType() != PacketType.Play.Client.BLOCK_PLACE) {
            return;
        }
        if (context.getData().getLastHorizontalDelta() > minMove && context.getData().getPitch() < minPitch) {
            alertManager.flag(context.getPlayer(), context.getData(), this, 1.0D,
                    "move=" + format(context.getData().getLastHorizontalDelta()) + ",pitch=" + format(context.getData().getPitch()));
        }
    }

    private static String format(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }
}
