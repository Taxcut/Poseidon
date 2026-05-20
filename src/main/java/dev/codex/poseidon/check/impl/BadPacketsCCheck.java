package dev.codex.poseidon.check.impl;

import com.comphenix.protocol.PacketType;
import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.packet.PacketContext;

public final class BadPacketsCCheck extends Check {
    private final AlertManager alertManager;

    public BadPacketsCCheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("BadPacketsC",
                plugin.getConfig().getBoolean("checks.BadPacketsC.enabled", true),
                plugin.getConfig().getDouble("checks.BadPacketsC.alert-vl", 1.0D));
        this.alertManager = alertManager;
    }

    @Override
    public void handle(PacketContext context) {
        if (context.getPacketType() != PacketType.Play.Client.USE_ENTITY || !context.isAttack()) {
            return;
        }

        if (context.getEntityId() == context.getPlayer().getEntityId()) {
            alertManager.flag(context.getPlayer(), context.getData(), this, 1.0D,
                    "self-attack");
        }
    }
}
