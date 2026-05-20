package dev.codex.poseidon.check.impl;

import com.comphenix.protocol.PacketType;
import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.check.CheckCategory;
import dev.codex.poseidon.check.CheckSeverity;
import dev.codex.poseidon.packet.PacketContext;

public final class FastPlaceCheck extends Check {
    private final AlertManager alertManager;
    private final int maxPlacesPerSecond;

    public FastPlaceCheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("FastPlace", plugin, true, 4.0D, CheckCategory.WORLD, CheckSeverity.MEDIUM, false,
                "Detects excessive block placement rate.");
        this.alertManager = alertManager;
        this.maxPlacesPerSecond = plugin.getConfig().getInt("checks.FastPlace.max-places-per-second", 16);
    }

    @Override
    public void handle(PacketContext context) {
        if (context.getPacketType() != PacketType.Play.Client.BLOCK_PLACE) {
            return;
        }
        int places = context.getData().recordBlockPlacePacket(context.getTimestamp());
        if (places > maxPlacesPerSecond) {
            alertManager.flag(context.getPlayer(), context.getData(), this,
                    Math.min(3.0D, (places - maxPlacesPerSecond) / 3.0D), "pps=" + places);
        }
    }
}
