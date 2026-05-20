package dev.codex.poseidon.check.impl;

import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.check.CheckCategory;
import dev.codex.poseidon.check.CheckSeverity;
import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.packet.PacketContext;

public final class InventoryMoveCheck extends Check {
    private final AlertManager alertManager;
    private final double maxHorizontal;
    private final long minOpenMs;

    public InventoryMoveCheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("InventoryMove", plugin, true, 4.0D, CheckCategory.INVENTORY, CheckSeverity.MEDIUM, false,
                "Detects sustained movement while a server inventory is open.");
        this.alertManager = alertManager;
        this.maxHorizontal = plugin.getConfig().getDouble("checks.InventoryMove.max-horizontal", 0.22D);
        this.minOpenMs = plugin.getConfig().getLong("checks.InventoryMove.min-open-ms", 150L);
    }

    @Override
    public void handle(PacketContext context) {
        if (!context.hasPosition()) {
            return;
        }
        PlayerData data = context.getData();
        if (data.isInventoryOpen()
                && context.getTimestamp() - data.getLastInventoryOpen() > minOpenMs
                && data.getLastHorizontalDelta() > maxHorizontal) {
            alertManager.flag(context.getPlayer(), data, this, 1.0D,
                    "move=" + format(data.getLastHorizontalDelta()));
        }
    }

    private static String format(double value) {
        return String.format(java.util.Locale.US, "%.3f", value);
    }
}
