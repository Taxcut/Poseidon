package dev.codex.poseidon.processor;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.data.PlayerDataManager;
import dev.codex.poseidon.replay.ReplayRecorder;
import org.bukkit.entity.Player;

public final class ServerPacketProcessor {
    private final PlayerDataManager dataManager;
    private final ReplayRecorder replayRecorder;

    public ServerPacketProcessor(PlayerDataManager dataManager, ReplayRecorder replayRecorder) {
        this.dataManager = dataManager;
        this.replayRecorder = replayRecorder;
    }

    public void process(PacketEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        PlayerData data = dataManager.getOrCreate(player);
        PacketType type = event.getPacketType();
        PacketContainer packet = event.getPacket();
        long now = System.currentTimeMillis();

        if (type == PacketType.Play.Server.ENTITY_VELOCITY) {
            processVelocity(player, data, packet, now);
        } else if (type == PacketType.Play.Server.POSITION) {
            processCorrection(data, now);
        }
    }

    private void processVelocity(Player player, PlayerData data, PacketContainer packet, long now) {
        Integer entityId = read(packet.getIntegers(), 0, null);
        if (entityId == null || entityId.intValue() != player.getEntityId()) {
            return;
        }

        int velocityX = read(packet.getIntegers(), 1, Integer.valueOf(0)).intValue();
        int velocityY = read(packet.getIntegers(), 2, Integer.valueOf(0)).intValue();
        int velocityZ = read(packet.getIntegers(), 3, Integer.valueOf(0)).intValue();

        double x = velocityX / 8000.0D;
        double y = velocityY / 8000.0D;
        double z = velocityZ / 8000.0D;
        data.recordVelocity(x, y, z, now);
        replayRecorder.recordVelocity(player, data, x, y, z, now);
    }

    private void processCorrection(PlayerData data, long now) {
        data.markPositionCorrection(now);
    }

    private static <T> T read(StructureModifier<T> modifier, int index, T fallback) {
        try {
            if (modifier == null || modifier.size() <= index) {
                return fallback;
            }
            T value = modifier.read(index);
            return value == null ? fallback : value;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
