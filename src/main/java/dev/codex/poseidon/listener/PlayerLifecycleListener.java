package dev.codex.poseidon.listener;

import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.data.PlayerDataManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class PlayerLifecycleListener implements Listener {
    private final PoseidonPlugin plugin;
    private final PlayerDataManager dataManager;

    public PlayerLifecycleListener(PoseidonPlugin plugin, PlayerDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerData data = dataManager.getOrCreate(event.getPlayer());
        data.setLastWorldName(event.getPlayer().getWorld().getName());
        data.markJoin(System.currentTimeMillis());
        dataManager.updateEntityMapping(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getActionManager().clear(event.getPlayer().getUniqueId());
        plugin.getAlertManager().clear(event.getPlayer().getUniqueId());
        plugin.getMitigationManager().clear(event.getPlayer().getUniqueId());
        plugin.getOnlineBehaviorModel().clear(event.getPlayer().getUniqueId());
        dataManager.remove(event.getPlayer());
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        plugin.getActionManager().clear(event.getPlayer().getUniqueId());
        plugin.getAlertManager().clear(event.getPlayer().getUniqueId());
        plugin.getMitigationManager().clear(event.getPlayer().getUniqueId());
        plugin.getOnlineBehaviorModel().clear(event.getPlayer().getUniqueId());
        dataManager.remove(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        PlayerData data = dataManager.getOrCreate(event.getPlayer());
        data.setLastWorldName(event.getPlayer().getWorld().getName());
        data.markTeleport(System.currentTimeMillis(),
                event.getPlayer().getLocation().getX(),
                event.getPlayer().getLocation().getY(),
                event.getPlayer().getLocation().getZ());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null) {
            return;
        }

        PlayerData data = dataManager.getOrCreate(event.getPlayer());
        data.markTeleport(System.currentTimeMillis(),
                event.getTo().getX(),
                event.getTo().getY(),
                event.getTo().getZ());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        PlayerData data = dataManager.getOrCreate(event.getPlayer());
        data.markRespawn(System.currentTimeMillis());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Player) {
            PlayerData data = dataManager.getOrCreate((org.bukkit.entity.Player) event.getEntity());
            data.markDamage(System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        PlayerData data = dataManager.getOrCreate(event.getPlayer());
        data.markBlockPlace(System.currentTimeMillis());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        PlayerData data = dataManager.getOrCreate(event.getPlayer());
        data.markBlockBreak(System.currentTimeMillis());
    }
}
