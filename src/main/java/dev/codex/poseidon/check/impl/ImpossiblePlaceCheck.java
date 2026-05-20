package dev.codex.poseidon.check.impl;

import com.comphenix.protocol.PacketType;
import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.check.CheckCategory;
import dev.codex.poseidon.check.CheckSeverity;
import dev.codex.poseidon.packet.PacketContext;

public final class ImpossiblePlaceCheck extends Check {
    private final AlertManager alertManager;
    private final double maxDistanceSquared;

    public ImpossiblePlaceCheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("ImpossiblePlace", plugin, true, 4.0D, CheckCategory.WORLD, CheckSeverity.HIGH, false,
                "Detects invalid block face or impossible block placement distance.");
        this.alertManager = alertManager;
        double maxDistance = plugin.getConfig().getDouble("checks.ImpossiblePlace.max-distance", 6.0D);
        this.maxDistanceSquared = maxDistance * maxDistance;
    }

    @Override
    public void handle(PacketContext context) {
        if (context.getPacketType() != PacketType.Play.Client.BLOCK_PLACE) {
            return;
        }
        BlockPlaceData place = BlockPlaceData.read(context.getPacket());
        if (!place.isValid()) {
            return;
        }
        boolean invalidFace = place.getFace() < -1 || place.getFace() > 5;
        boolean tooFar = place.distanceSquared(context.getData().getX(), context.getData().getY() + 1.62D, context.getData().getZ()) > maxDistanceSquared;
        if (invalidFace || tooFar) {
            alertManager.flag(context.getPlayer(), context.getData(), this, 1.0D,
                    "face=" + place.getFace() + ",far=" + tooFar);
        }
    }
}
