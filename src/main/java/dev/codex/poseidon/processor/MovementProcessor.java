package dev.codex.poseidon.processor;

import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.packet.PacketContext;

public final class MovementProcessor {
    public void process(PacketContext context) {
        PlayerData data = context.getData();
        if (context.hasPosition() && context.hasRotation()) {
            data.updatePositionAndRotation(context.getX(), context.getY(), context.getZ(),
                    context.getYaw(), context.getPitch(), context.isOnGround(), context.getTimestamp());
        } else if (context.hasPosition()) {
            data.updatePosition(context.getX(), context.getY(), context.getZ(), context.isOnGround(), context.getTimestamp());
        } else if (context.hasRotation()) {
            data.updateRotation(context.getYaw(), context.getPitch(), context.isOnGround());
        } else if (context.hasGround()) {
            data.updateGround(context.isOnGround());
        }
    }
}
