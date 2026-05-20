package dev.codex.poseidon.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import dev.codex.poseidon.data.PlayerData;
import org.bukkit.entity.Player;

public final class PacketContext {
    private final Player player;
    private final PlayerData data;
    private final PacketType packetType;
    private final PacketContainer packet;
    private final long timestamp;
    private final boolean hasPosition;
    private final boolean hasRotation;
    private final boolean hasGround;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    private final boolean onGround;
    private final int entityId;
    private final EnumWrappers.EntityUseAction entityUseAction;

    public PacketContext(Player player,
                         PlayerData data,
                         PacketType packetType,
                         PacketContainer packet,
                         long timestamp,
                         boolean hasPosition,
                         boolean hasRotation,
                         boolean hasGround,
                         double x,
                         double y,
                         double z,
                         float yaw,
                         float pitch,
                         boolean onGround,
                         int entityId,
                         EnumWrappers.EntityUseAction entityUseAction) {
        this.player = player;
        this.data = data;
        this.packetType = packetType;
        this.packet = packet;
        this.timestamp = timestamp;
        this.hasPosition = hasPosition;
        this.hasRotation = hasRotation;
        this.hasGround = hasGround;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
        this.entityId = entityId;
        this.entityUseAction = entityUseAction;
    }

    public Player getPlayer() {
        return player;
    }

    public PlayerData getData() {
        return data;
    }

    public PacketType getPacketType() {
        return packetType;
    }

    public PacketContainer getPacket() {
        return packet;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean hasPosition() {
        return hasPosition;
    }

    public boolean hasRotation() {
        return hasRotation;
    }

    public boolean hasGround() {
        return hasGround;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public int getEntityId() {
        return entityId;
    }

    public EnumWrappers.EntityUseAction getEntityUseAction() {
        return entityUseAction;
    }

    public boolean isAttack() {
        return entityUseAction == EnumWrappers.EntityUseAction.ATTACK;
    }
}
