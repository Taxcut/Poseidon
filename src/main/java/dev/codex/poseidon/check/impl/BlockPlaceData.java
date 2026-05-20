package dev.codex.poseidon.check.impl;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.BlockPosition;

final class BlockPlaceData {
    private final int x;
    private final int y;
    private final int z;
    private final int face;
    private final boolean valid;

    private BlockPlaceData(int x, int y, int z, int face, boolean valid) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.face = face;
        this.valid = valid;
    }

    static BlockPlaceData read(PacketContainer packet) {
        BlockPosition position = read(packet.getBlockPositionModifier(), 0, null);
        int x = 0;
        int y = 0;
        int z = 0;
        boolean valid = position != null;
        if (position != null) {
            x = position.getX();
            y = position.getY();
            z = position.getZ();
        } else {
            x = read(packet.getIntegers(), 0, Integer.valueOf(0)).intValue();
            y = read(packet.getIntegers(), 1, Integer.valueOf(0)).intValue();
            z = read(packet.getIntegers(), 2, Integer.valueOf(0)).intValue();
            valid = true;
        }
        int face = read(packet.getIntegers(), 3, Integer.valueOf(-1)).intValue();
        return new BlockPlaceData(x, y, z, face, valid);
    }

    int getX() {
        return x;
    }

    int getY() {
        return y;
    }

    int getZ() {
        return z;
    }

    int getFace() {
        return face;
    }

    boolean isValid() {
        return valid;
    }

    double distanceSquared(double x, double y, double z) {
        double dx = this.x + 0.5D - x;
        double dy = this.y + 0.5D - y;
        double dz = this.z + 0.5D - z;
        return dx * dx + dy * dy + dz * dz;
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
