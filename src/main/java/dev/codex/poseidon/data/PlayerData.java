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
    private final Queue<Long> attackDelays = new ArrayDeque<Long>();
    private final Queue<Long> inventoryClickPackets = new ArrayDeque<Long>();
    private final Queue<Long> blockPlacePackets = new ArrayDeque<Long>();
    private final Queue<Long> allPackets = new ArrayDeque<Long>();
    private final Queue<PositionSnapshot> positionHistory = new ArrayDeque<PositionSnapshot>();
    private final Queue<RotationSample> rotationSamples = new ArrayDeque<RotationSample>();
    private final AtomicInteger transactionCounter = new AtomicInteger(1);

    private volatile long transactionPing = -1L;
    private volatile long lastTransactionSent;
    private volatile long lastTransactionReceived;
    private volatile long lastFlyingTimestamp;
    private volatile long lastPositionTimestamp;
    private volatile long lastAttackTimestamp;
    private volatile long lastAttackPacketTimestamp;
    private volatile long lastPacketTimestamp;
    private volatile long lastInventoryOpen;
    private volatile long lastInventoryClose;
    private volatile long lastTeleportTimestamp;
    private volatile int lastAttackedEntityId = -1;
    private volatile String lastWorldName = "";
    private volatile VelocitySnapshot lastVelocity;
    private volatile long lastVelocityFlag;
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
    private volatile boolean sprinting;
    private volatile boolean sneaking;
    private volatile boolean inventoryOpen;
    private volatile boolean latencyCompensated;
    private volatile long lastLatencyCompensation;
    private volatile long lastLatencySignal;
    private volatile boolean inLiquid;
    private volatile boolean inWeb;
    private volatile boolean onIce;
    private volatile boolean onClimbable;
    private volatile boolean onSoulSand;
    private volatile boolean onSlime;
    private volatile boolean nearComplexCollision;

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
        recordRotationSample(yaw - this.yaw, pitch - this.pitch, timestamp);
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
        recordRotationSample(yaw - this.yaw, pitch - this.pitch, System.currentTimeMillis());
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
        return addViolation(checkName, amount, 100.0D);
    }

    public double addViolation(String checkName, double amount, double maxViolation) {
        synchronized (violations) {
            double value = amount;
            if (violations.containsKey(checkName)) {
                value += violations.get(checkName).doubleValue();
            }
            if (maxViolation > 0.0D) {
                value = Math.min(maxViolation, value);
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

    public void resetViolations() {
        synchronized (violations) {
            violations.clear();
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
        if (lastAttackPacketTimestamp > 0L) {
            recordRollingPacket(attackDelays, Math.max(0L, timestamp - lastAttackPacketTimestamp), 50, Long.MAX_VALUE);
        }
        lastAttackPacketTimestamp = timestamp;
        return recordRollingPacket(attackPackets, timestamp);
    }

    public int recordInventoryClickPacket(long timestamp) {
        return recordRollingPacket(inventoryClickPackets, timestamp);
    }

    public int recordBlockPlacePacket(long timestamp) {
        return recordRollingPacket(blockPlacePackets, timestamp);
    }

    public int recordAnyPacket(long timestamp) {
        lastPacketTimestamp = timestamp;
        return recordRollingPacket(allPackets, timestamp);
    }

    public ClickStats getClickStats() {
        synchronized (attackDelays) {
            return ClickStats.from(attackDelays);
        }
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
        this.sprinting = player.isSprinting();
        this.sneaking = player.isSneaking();
    }

    public void updateEnvironmentState(boolean inLiquid, boolean inWeb, boolean onIce, boolean onClimbable, boolean onSoulSand) {
        this.inLiquid = inLiquid;
        this.inWeb = inWeb;
        this.onIce = onIce;
        this.onClimbable = onClimbable;
        this.onSoulSand = onSoulSand;
    }

    public void updateEnvironmentState(boolean inLiquid,
                                       boolean inWeb,
                                       boolean onIce,
                                       boolean onClimbable,
                                       boolean onSoulSand,
                                       boolean onSlime,
                                       boolean nearComplexCollision) {
        updateEnvironmentState(inLiquid, inWeb, onIce, onClimbable, onSoulSand);
        this.onSlime = onSlime;
        this.nearComplexCollision = nearComplexCollision;
    }

    public void recordVelocity(double x, double y, double z, long timestamp) {
        this.lastVelocity = new VelocitySnapshot(x, y, z, timestamp);
    }

    public VelocitySnapshot getLastVelocity() {
        return lastVelocity;
    }

    public boolean canVelocityFlag(long timestamp, long intervalMs) {
        if (timestamp - lastVelocityFlag < intervalMs) {
            return false;
        }
        lastVelocityFlag = timestamp;
        return true;
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

    public boolean isSprinting() {
        return sprinting;
    }

    public boolean isSneaking() {
        return sneaking;
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

    public boolean isOnSlime() {
        return onSlime;
    }

    public boolean isNearComplexCollision() {
        return nearComplexCollision;
    }

    public boolean isInventoryOpen() {
        return inventoryOpen;
    }

    public long getLastInventoryOpen() {
        return lastInventoryOpen;
    }

    public long getLastInventoryClose() {
        return lastInventoryClose;
    }

    public void setInventoryOpen(boolean inventoryOpen, long timestamp) {
        this.inventoryOpen = inventoryOpen;
        if (inventoryOpen) {
            this.lastInventoryOpen = timestamp;
        } else {
            this.lastInventoryClose = timestamp;
        }
    }

    public long getLastPacketTimestamp() {
        return lastPacketTimestamp;
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

    public RotationStats getRotationStats() {
        synchronized (rotationSamples) {
            return RotationStats.from(rotationSamples);
        }
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

    private void recordRotationSample(double yawDelta, double pitchDelta, long timestamp) {
        if (!hasPosition && lastPositionTimestamp <= 0L) {
            return;
        }
        synchronized (rotationSamples) {
            rotationSamples.add(new RotationSample(wrapDegrees(yawDelta), pitchDelta, timestamp));
            long cutoff = timestamp - 3000L;
            while (!rotationSamples.isEmpty() && rotationSamples.peek().timestamp < cutoff) {
                rotationSamples.poll();
            }
            while (rotationSamples.size() > 80) {
                rotationSamples.poll();
            }
        }
    }

    private static double wrapDegrees(double value) {
        value %= 360.0D;
        if (value >= 180.0D) {
            value -= 360.0D;
        }
        if (value < -180.0D) {
            value += 360.0D;
        }
        return value;
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
        return recordRollingPacket(queue, timestamp, Integer.MAX_VALUE, 1000L);
    }

    private static int recordRollingPacket(Queue<Long> queue, long timestamp, int maxSize, long windowMs) {
        synchronized (queue) {
            queue.add(timestamp);
            if (windowMs != Long.MAX_VALUE) {
                long cutoff = timestamp - windowMs;
                while (!queue.isEmpty() && queue.peek() < cutoff) {
                    queue.poll();
                }
            }
            while (queue.size() > maxSize) {
                queue.poll();
            }
            return queue.size();
        }
    }

    private static final class RotationSample {
        private final double yawDelta;
        private final double pitchDelta;
        private final long timestamp;

        private RotationSample(double yawDelta, double pitchDelta, long timestamp) {
            this.yawDelta = yawDelta;
            this.pitchDelta = pitchDelta;
            this.timestamp = timestamp;
        }
    }

    public static final class ClickStats {
        private final int samples;
        private final double average;
        private final double variance;
        private final double standardDeviation;
        private final double duplicateRatio;

        private ClickStats(int samples, double average, double variance, double standardDeviation, double duplicateRatio) {
            this.samples = samples;
            this.average = average;
            this.variance = variance;
            this.standardDeviation = standardDeviation;
            this.duplicateRatio = duplicateRatio;
        }

        private static ClickStats from(Queue<Long> delays) {
            int size = delays.size();
            if (size == 0) {
                return new ClickStats(0, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            double sum = 0.0D;
            Long last = null;
            int duplicates = 0;
            for (Long delay : delays) {
                sum += delay.longValue();
                if (last != null && Math.abs(delay.longValue() - last.longValue()) <= 1L) {
                    duplicates++;
                }
                last = delay;
            }
            double average = sum / size;
            double variance = 0.0D;
            for (Long delay : delays) {
                double offset = delay.longValue() - average;
                variance += offset * offset;
            }
            variance /= size;
            return new ClickStats(size, average, variance, Math.sqrt(variance), size <= 1 ? 0.0D : duplicates / (double) (size - 1));
        }

        public int getSamples() {
            return samples;
        }

        public double getAverage() {
            return average;
        }

        public double getVariance() {
            return variance;
        }

        public double getStandardDeviation() {
            return standardDeviation;
        }

        public double getDuplicateRatio() {
            return duplicateRatio;
        }
    }

    public static final class RotationStats {
        private final int samples;
        private final double maxYawDelta;
        private final double maxPitchDelta;
        private final double yawVariance;
        private final double pitchVariance;
        private final double duplicateRatio;

        private RotationStats(int samples,
                              double maxYawDelta,
                              double maxPitchDelta,
                              double yawVariance,
                              double pitchVariance,
                              double duplicateRatio) {
            this.samples = samples;
            this.maxYawDelta = maxYawDelta;
            this.maxPitchDelta = maxPitchDelta;
            this.yawVariance = yawVariance;
            this.pitchVariance = pitchVariance;
            this.duplicateRatio = duplicateRatio;
        }

        private static RotationStats from(Queue<RotationSample> samples) {
            int size = samples.size();
            if (size == 0) {
                return new RotationStats(0, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            double yawSum = 0.0D;
            double pitchSum = 0.0D;
            double maxYaw = 0.0D;
            double maxPitch = 0.0D;
            RotationSample last = null;
            int duplicates = 0;
            for (RotationSample sample : samples) {
                double absYaw = Math.abs(sample.yawDelta);
                double absPitch = Math.abs(sample.pitchDelta);
                yawSum += absYaw;
                pitchSum += absPitch;
                maxYaw = Math.max(maxYaw, absYaw);
                maxPitch = Math.max(maxPitch, absPitch);
                if (last != null
                        && Math.abs(absYaw - Math.abs(last.yawDelta)) < 0.001D
                        && Math.abs(absPitch - Math.abs(last.pitchDelta)) < 0.001D) {
                    duplicates++;
                }
                last = sample;
            }
            double yawAverage = yawSum / size;
            double pitchAverage = pitchSum / size;
            double yawVariance = 0.0D;
            double pitchVariance = 0.0D;
            for (RotationSample sample : samples) {
                double yawOffset = Math.abs(sample.yawDelta) - yawAverage;
                double pitchOffset = Math.abs(sample.pitchDelta) - pitchAverage;
                yawVariance += yawOffset * yawOffset;
                pitchVariance += pitchOffset * pitchOffset;
            }
            yawVariance /= size;
            pitchVariance /= size;
            return new RotationStats(size, maxYaw, maxPitch, yawVariance, pitchVariance,
                    size <= 1 ? 0.0D : duplicates / (double) (size - 1));
        }

        public int getSamples() {
            return samples;
        }

        public double getMaxYawDelta() {
            return maxYawDelta;
        }

        public double getMaxPitchDelta() {
            return maxPitchDelta;
        }

        public double getYawVariance() {
            return yawVariance;
        }

        public double getPitchVariance() {
            return pitchVariance;
        }

        public double getDuplicateRatio() {
            return duplicateRatio;
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
