package dev.codex.poseidon.processor;

import com.comphenix.protocol.PacketType;
import dev.codex.poseidon.packet.PacketContext;

public final class CombatProcessor {
    public void process(PacketContext context) {
        if (context.getPacketType() == PacketType.Play.Client.USE_ENTITY
                && context.isAttack()
                && context.getEntityId() >= 0) {
            context.getData().recordAttack(context.getEntityId(), context.getTimestamp());
        }
    }
}
