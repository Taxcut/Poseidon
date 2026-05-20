package dev.codex.poseidon.data;

import dev.codex.poseidon.mitigation.MitigationLevel;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class PlayerData {
    private final UUID uuid;
    private final String name;
    private final ConcurrentMap<Short, Long> pendingTransactions = new ConcurrentHashMap<Short, Long>();
    private final Map<String, Double> violations = Collections.synchronizedMap(new HashMap<String, Double>());
    private final Map<String, Long> lastAlerts = Collections.synchronizedMap(new HashMap<String, Long>());
    private final Queue<Long> flyingPackets = new ArrayDeque<Long>();
    private final Queue<Long> attackPackets = new ArrayDeque<Long>();
    private final Queue<Long> inventoryClickPackets = new ArrayDeque<Long>();
    private final Queue<Long> blockPlacePackets = new ArrayDeque<Long>();
    private final Queue<PositionSnapshot> positionHistory = new ArrayDeque<PositionSnapshot>();
    private final AtomicInteger transactionCounter = new AtomicInteger(1);

    private volatile long transactionPing = -1L;
    private volatile long lastTransactionSent;
    private volatile long lastTransactionReceived;
    private volatile long lastFlyingTimestamp;
    private volatile long lastPositionTimestamp;
    private volatile long lastAttackTimestamp;
    private volatile long lastTeleportTimestamp;
    private volatile int lastAttackedEntityId = -1;
    private volatile String lastWorldName = "";
    private volatile VelocitySnapshot lastVelocity;
    private volatile MitigationLevel mitigationLevel = MitigationLevel.NONE;
    private volatile double mitigationScore;
    private volatile long mitigationExpiresAt;
    private volatile double modelScore;
    private volatile double modelConfidence;
    private volatile long lastModelScoreUpdate;
    private volatile long lastModelAlert;
    private volatile String modelBreakdown = "none";
    private volatile double crossInstanceRisk;
    private volatile int crossInstanceRecords;
    private volatile long lastCrossInstanceUpdate;
    private volatile int speedAmplifier = -1;
    private volatile int jumpAmplifier = -1;
    private volatile boolean flying;
    private volatile boolean allowFlight;
    private volatile boolean insideVehicle;
    private volatile boolean latencyCompensated;
    private volatile long lastLatencyCompensation;
    private volatile long lastLatencySignal;
    private volatile boolean inLiquid;
    private volatile boolean inWeb;
    private volatile boolean onIce;
    private volatile boolean onClimbable;
    private volatile boolean onSoulSand;

    private volatile boolean onGround;
    private volatile boolean lastOnGround;
    private volatile boolean hasPosition;
    private volatile double x;
    private volatile double y;
    private volatile double z;
    private volatile double lastX;
    private volatile double lastY;
    private volatile double lastZ;
    private volatile double lastDeltaX;
    private volatile double lastDeltaY;
    private volatile double lastDeltaZ;
    private volatile double lastHorizontalDelta;
    private volatile float yaw;
    private volatile float pitch;
    private volatile float lastYaw;
    private volatile float lastPitch;

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void updatePosition(double x, double y, double z, boolean onGround, long timestamp) {
        this.lastX = this.x;
        this.lastY = this.y;
        this.lastZ = this.z;
        this.lastDeltaX = x - this.x;
        this.lastDeltaY = y - this.y;
        this.lastDeltaZ = z - this.z;
        this.lastHorizontalDelta = Math.sqrt(lastDeltaX * lastDeltaX + lastDeltaZ * lastDeltaZ);
        this.lastOnGround = this.onGround;
        this.x = x;
        this.y = y;
        this.z = z;
        this.onGround = onGround;
        this.hasPosition = true;
        this.lastPositionTimestamp = timestamp;
        recordPositionSnapshot(x, y, z, this.yaw, this.pitch, onGround, timestamp);
    }

    public void updatePositionAndRotation(double x, double y, double z, float yaw, float pitch, boolean onGround, long timestamp) {
        this.lastX = this.x;
        this.lastY = this.y;
        this.lastZ = this.z;
        this.lastDeltaX = x - this.x;
        this.lastDeltaY = y - this.y;
        this.lastDeltaZ = z - this.z;
        this.lastHorizontalDelta = Math.sqrt(lastDeltaX * lastDeltaX + lastDeltaZ * lastDeltaZ);
        this.lastYaw = this.yaw;
        this.lastPitch = this.pitch;
        this.lastOnGround = this.onGround;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
        this.hasPosition = true;
        this.lastPositionTimestamp = timestamp;
        recordPositionSnapshot(x, y, z, yaw, pitch, onGround, timestamp);
    }

    public void updateRotation(float yaw, float pitch, boolean onGround) {
        this.lastYaw = this.yaw;
        this.lastPitch = this.pitch;
        this.yaw = yaw;
        this.pitch = pitch;
        this.lastOnGround = this.onGround;
        this.onGround = onGround;
    }

    public void updateGround(boolean onGround) {
        this.lastOnGround = this.onGround;
        this.onGround = onGround;
    }

    public int recordFlyingPacket(long timestamp) {
        synchronized (flyingPackets) {
            flyingPackets.add(timestamp);
            long cutoff = timestamp - 1000L;
            while (!flyingPackets.isEmpty() && flyingPackets.peek() < cutoff) {
                flyingPackets.poll();
            }
            lastFlyingTimestamp = timestamp;
            return flyingPackets.size();
        }
    }

    public short nextTransactionId() {
        int value = transactionCounter.getAndIncrement();
        if (value > Short.MAX_VALUE - 1000) {
            transactionCounter.set(1);
        }
        return (short) -Math.abs(value);
    }

    public void addTransaction(short id, long timestamp) {
        pendingTransactions.put(id, timestamp);
        lastTransactionSent = timestamp;
    }

    public boolean confirmTransaction(short id, long timestamp) {
        Long sent = pendingTransactions.remove(id);
        if (sent == null) {
            return false;
        }
        transactionPing = Math.max(0L, timestamp - sent.longValue());
        lastTransactionReceived = timestamp;
        return true;
    }

    public void purgeStaleTransactions(long now, long maxAgeMs) {
        Iterator<Map.Entry<Short, Long>> iterator = pendingTransactions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Short, Long> entry = iterator.next();
            if (now - entry.getValue().longValue() > maxAgeMs) {
                iterator.remove();
            }
        }
    }

    public double addViolation(String checkName, double amount) {
        synchronized (violations) {
            double value = amount;
            if (violations.containsKey(checkName)) {
                value += violations.get(checkName).doubleValue();
            }
            violations.put(checkName, value);
            return value;
        }
    }

    public void decayViolation(String checkName, double amount) {
        synchronized (violations) {
            Double current = violations.get(checkName);
            if (current == null) {
                return;
            }
            double next = current.doubleValue() - amount;
            if (next <= 0.0D) {
                violations.remove(checkName);
            } else {
                violations.put(checkName, next);
            }
        }
    }

    public void decayAllViolations(double amount) {
        if (amount <= 0.0D) {
            return;
        }

        synchronized (violations) {
            Iterator<Map.Entry<String, Double>> iterator = violations.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Double> entry = iterator.next();
                double next = entry.getValue().doubleValue() - amount;
                if (next <= 0.0D) {
                    iterator.remove();
                } else {
                    entry.setValue(next);
                }
            }
        }
    }

    public boolean canAlert(String checkName, long now, long cooldownMs) {
        synchronized (lastAlerts) {
            Long last = lastAlerts.get(checkName);
            if (last != null && now - last.longValue() < cooldownMs) {
                return false;
            }
            lastAlerts.put(checkName, now);
            return true;
        }
    }

    public Map<String, Double> getViolations() {
        synchronized (violations) {
            return new HashMap<String, Double>(violations);
        }
    }

    public PositionSnapshot getPositionAtOrBefore(long timestamp) {
        synchronized (positionHistory) {
            PositionSnapshot best = null;
            for (PositionSnapshot snapshot : positionHistory) {
                if (snapshot.getTimestamp() <= timestamp) {
                    best = snapshot;
                } else {
                    break;
                }
            }

            if (best != null) {
                return best;
            }
            return positionHistory.peek();
        }
    }

    public int getPositionHistorySize() {
        synchronized (positionHistory) {
            return positionHistory.size();
        }
    }

    public long getTransactionPing() {
        return transactionPing;
    }

    public long getLastTransactionSent() {
        return lastTransactionSent;
    }

    public long getLastTransactionReceived() {
        return lastTransactionReceived;
    }

    public int getPendingTransactionCount() {
        return pendingTransactions.size();
    }

    public long getLastFlyingTimestamp() {
        return lastFlyingTimestamp;
    }

    public long getLastPositionTimestamp() {
        return lastPositionTimestamp;
    }

    public long getLastAttackTimestamp() {
        return lastAttackTimestamp;
    }

    public void recordAttack(int entityId, long timestamp) {
        this.lastAttackedEntityId = entityId;
        this.lastAttackTimestamp = timestamp;
    }

    public int recordAttackPacket(long timestamp) {
        return recordRollingPacket(attackPackets, timestamp);
    }

    public int recordInventoryClickPacket(long timestamp) {
        return recordRollingPacket(inventoryClickPackets, timestamp);
    }

    public int recordBlockPlacePacket(long timestamp) {
        return recordRollingPacket(blockPlacePackets, timestamp);
    }

    public int getLastAttackedEntityId() {
        return lastAttackedEntityId;
    }

    public long getLastTeleportTimestamp() {
        return lastTeleportTimestamp;
    }

    public void markTeleport(long timestamp, double x, double y, double z) {
        this.lastTeleportTimestamp = timestamp;
        this.lastX = x;
        this.lastY = y;
        this.lastZ = z;
        this.lastDeltaX = 0.0D;
        this.lastDeltaY = 0.0D;
        this.lastDeltaZ = 0.0D;
        this.lastHorizontalDelta = 0.0D;
        this.x = x;
        this.y = y;
        this.z = z;
        this.hasPosition = true;
        this.lastPositionTimestamp = timestamp;
        recordPositionSnapshot(x, y, z, this.yaw, this.pitch, this.onGround, timestamp);
    }

    public void markPositionCorrection(long timestamp) {
        this.lastTeleportTimestamp = timestamp;
    }

    public void setLatencyCompensated(boolean latencyCompensated, long timestamp) {
        this.latencyCompensated = latencyCompensated;
        if (latencyCompensated) {
            this.lastLatencyCompensation = timestamp;
        }
    }

    public void updateRuntimeState(Player player) {
        this.speedAmplifier = amplifier(player, PotionEffectType.SPEED);
        this.jumpAmplifier = amplifier(player, PotionEffectType.JUMP);
        this.flying = player.isFlying();
        this.allowFlight = player.getAllowFlight();
        this.insideVehicle = player.isInsideVehicle();
    }

    public void updateEnvironmentState(boolean inLiquid, boolean inWeb, boolean onIce, boolean onClimbable, boolean onSoulSand) {
        this.inLiquid = inLiquid;
        this.inWeb = inWeb;
        this.onIce = onIce;
        this.onClimbable = onClimbable;
        this.onSoulSand = onSoulSand;
    }

    public void recordVelocity(double x, double y, double z, long timestamp) {
        this.lastVelocity = new VelocitySnapshot(x, y, z, timestamp);
    }

    public VelocitySnapshot getLastVelocity() {
        return lastVelocity;
    }

    public String getLastVelocitySummary() {
        VelocitySnapshot snapshot = lastVelocity;
        if (snapshot == null) {
            return "none";
        }
        return String.format(java.util.Locale.US, "%.3f, %.3f, %.3f (%dms ago)",
                snapshot.getX(), snapshot.getY(), snapshot.getZ(),
                Math.max(0L, System.currentTimeMillis() - snapshot.getTimestamp()));
    }

    public MitigationLevel getMitigationLevel() {
        if (mitigationLevel != MitigationLevel.NONE && System.currentTimeMillis() > mitigationExpiresAt) {
            return MitigationLevel.NONE;
        }
        return mitigationLevel;
    }

    public double getMitigationScore() {
        return mitigationScore;
    }

    public void setMitigation(MitigationLevel level, double score, long expiresAt) {
        this.mitigationLevel = level;
        this.mitigationScore = Math.max(0.0D, score);
        this.mitigationExpiresAt = expiresAt;
    }

    public void clearMitigation() {
        this.mitigationLevel = MitigationLevel.NONE;
        this.mitigationScore = 0.0D;
        this.mitigationExpiresAt = 0L;
    }

    public int getSpeedAmplifier() {
        return speedAmplifier;
    }

    public int getJumpAmplifier() {
        return jumpAmplifier;
    }

    public boolean isFlying() {
        return flying;
    }

    public boolean isAllowFlight() {
        return allowFlight;
    }

    public boolean isInsideVehicle() {
        return insideVehicle;
    }

    public boolean isLatencyCompensated() {
        return latencyCompensated && System.currentTimeMillis() - lastLatencyCompensation < 5000L;
    }

    public long getLastLatencyCompensation() {
        return lastLatencyCompensation;
    }

    public boolean canLatencySignal(long now, long intervalMs) {
        if (now - lastLatencySignal < intervalMs) {
            return false;
        }
        lastLatencySignal = now;
        return true;
    }

    public double getModelScore() {
        return modelScore;
    }

    public double getModelConfidence() {
        return modelConfidence;
    }

    public long getLastModelScoreUpdate() {
        return lastModelScoreUpdate;
    }

    public String getModelBreakdown() {
        return modelBreakdown;
    }

    public void setModelScore(double score, double confidence, long timestamp, String breakdown) {
        this.modelScore = Math.max(0.0D, Math.min(1.0D, score));
        this.modelConfidence = Math.max(0.0D, Math.min(1.0D, confidence));
        this.lastModelScoreUpdate = timestamp;
        this.modelBreakdown = breakdown == null || breakdown.isEmpty() ? "none" : breakdown;
    }

    public boolean canModelAlert(long now, long intervalMs) {
        if (now - lastModelAlert < intervalMs) {
            return false;
        }
        lastModelAlert = now;
        return true;
    }

    public double getCrossInstanceRisk() {
        return crossInstanceRisk;
    }

    public int getCrossInstanceRecords() {
        return crossInstanceRecords;
    }

    public long getLastCrossInstanceUpdate() {
        return lastCrossInstanceUpdate;
    }

    public void setCrossInstanceRisk(double risk, int records, long timestamp) {
        this.crossInstanceRisk = Math.max(0.0D, risk);
        this.crossInstanceRecords = Math.max(0, records);
        this.lastCrossInstanceUpdate = timestamp;
    }

    public boolean isInLiquid() {
        return inLiquid;
    }

    public boolean isInWeb() {
        return inWeb;
    }

    public boolean isOnIce() {
        return onIce;
    }

    public boolean isOnClimbable() {
        return onClimbable;
    }

    public boolean isOnSoulSand() {
        return onSoulSand;
    }

    public String getLastWorldName() {
        return lastWorldName;
    }

    public void setLastWorldName(String lastWorldName) {
        this.lastWorldName = lastWorldName;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public boolean wasOnGround() {
        return lastOnGround;
    }

    public boolean hasPosition() {
        return hasPosition;
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

    public double getLastX() {
        return lastX;
    }

    public double getLastY() {
        return lastY;
    }

    public double getLastZ() {
        return lastZ;
    }

    public double getLastDeltaX() {
        return lastDeltaX;
    }

    public double getLastDeltaY() {
        return lastDeltaY;
    }

    public double getLastDeltaZ() {
        return lastDeltaZ;
    }

    public double getLastHorizontalDelta() {
        return lastHorizontalDelta;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public float getLastYaw() {
        return lastYaw;
    }

    public float getLastPitch() {
        return lastPitch;
    }

    private void recordPositionSnapshot(double x, double y, double z, float yaw, float pitch, boolean onGround, long timestamp) {
        synchronized (positionHistory) {
            positionHistory.add(new PositionSnapshot(x, y, z, yaw, pitch, onGround, timestamp));
            long cutoff = timestamp - 4000L;
            while (!positionHistory.isEmpty() && positionHistory.peek().getTimestamp() < cutoff) {
                positionHistory.poll();
            }
        }
    }

    private static int amplifier(Player player, PotionEffectType type) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().equals(type)) {
                return effect.getAmplifier();
            }
        }
        return -1;
    }

    private static int recordRollingPacket(Queue<Long> queue, long timestamp) {
        synchronized (queue) {
            queue.add(timestamp);
            long cutoff = timestamp - 1000L;
            while (!queue.isEmpty() && queue.peek() < cutoff) {
                queue.poll();
            }
            return queue.size();
        }
    }

    public static final class PositionSnapshot {
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final float pitch;
        private final boolean onGround;
        private final long timestamp;

        private PositionSnapshot(double x, double y, double z, float yaw, float pitch, boolean onGround, long timestamp) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.onGround = onGround;
            this.timestamp = timestamp;
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

        public long getTimestamp() {
            return timestamp;
        }
    }

    public static final class VelocitySnapshot {
        private final double x;
        private final double y;
        private final double z;
        private final long timestamp;

        private VelocitySnapshot(double x, double y, double z, long timestamp) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.timestamp = timestamp;
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

        public long getTimestamp() {
            return timestamp;
        }
    }
}
