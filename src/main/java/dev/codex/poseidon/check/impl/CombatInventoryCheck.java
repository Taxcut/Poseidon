package dev.codex.poseidon.check.impl;

import com.comphenix.protocol.PacketType;
import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.check.CheckCategory;
import dev.codex.poseidon.check.CheckSeverity;
import dev.codex.poseidon.packet.PacketContext;

public final class CombatInventoryCheck extends Check {
    private final AlertManager alertManager;
    private final long recentOpenMs;

    public CombatInventoryCheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("CombatInventory", plugin, true, 4.0D, CheckCategory.INVENTORY, CheckSeverity.MEDIUM, false,
                "Detects attacking while server inventory state is open.");
        this.alertManager = alertManager;
        this.recentOpenMs = plugin.getConfig().getLong("checks.CombatInventory.recent-open-ms", 350L);
    }

    @Override
    public void handle(PacketContext context) {
        if (context.getPacketType() != PacketType.Play.Client.USE_ENTITY || !context.isAttack()) {
            return;
        }
        if (context.getData().isInventoryOpen()
                || context.getTimestamp() - context.getData().getLastInventoryOpen() < recentOpenMs) {
            alertManager.flag(context.getPlayer(), context.getData(), this, 1.0D,
                    "open=" + context.getData().isInventoryOpen());
        }
    }
}
