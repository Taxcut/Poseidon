package dev.codex.poseidon.exempt;

import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.data.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public final class ExemptionManager {
    private final PoseidonPlugin plugin;

    public ExemptionManager(PoseidonPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isMovementExempt(Player player, PlayerData data, long now) {
        return isGeneralExempt(player, data, now)
                || data.isInLiquid()
                || data.isInWeb()
                || data.isOnClimbable()
                || data.isNearComplexCollision()
                || recently(now, data.getLastBlockPlaceTimestamp(), "exemptions.block-place-ms")
                || recently(now, data.getLastBlockBreakTimestamp(), "exemptions.block-break-ms")
                || data.isLatencyCompensated();
    }

    public boolean isCombatExempt(Player player, PlayerData data, long now) {
        return isGeneralExempt(player, data, now)
                || recently(now, data.getLastDamageTimestamp(), "exemptions.damage-ms")
                || data.isLatencyCompensated();
    }

    public boolean isVelocityExempt(Player player, PlayerData data, long now) {
        return isGeneralExempt(player, data, now)
                || data.isInLiquid()
                || data.isInWeb()
                || data.isOnClimbable()
                || data.isOnSlime()
                || data.isNearComplexCollision()
                || data.isLatencyCompensated();
    }

    public boolean isGeneralExempt(Player player, PlayerData data, long now) {
        if (player == null || data == null || !player.isOnline()) {
            return true;
        }
        if (player.hasPermission("poseidon.bypass")) {
            return true;
        }
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return true;
        }
        if (data.isFlying() || data.isAllowFlight() || data.isInsideVehicle()) {
            return true;
        }
        if (plugin.getTpsTracker() != null && plugin.getTpsTracker().isLagging()) {
            return true;
        }
        if (data.getTransactionPing() > plugin.getConfig().getLong("exemptions.high-transaction-ping-ms", 350L)) {
            return true;
        }
        return recently(now, data.getJoinTimestamp(), "exemptions.join-ms")
                || recently(now, data.getLastTeleportTimestamp(), "exemptions.teleport-ms")
                || recently(now, data.getLastRespawnTimestamp(), "exemptions.respawn-ms");
    }

    private boolean recently(long now, long timestamp, String path) {
        return timestamp > 0L && now - timestamp < plugin.getConfig().getLong(path, 1000L);
    }
}
