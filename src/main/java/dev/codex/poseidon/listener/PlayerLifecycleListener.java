package dev.codex.poseidon.listener;

import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.data.PlayerDataManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class PlayerLifecycleListener implements Listener {
    private final PlayerDataManager dataManager;

    public PlayerLifecycleListener(PlayerDataManager dataManager) {
        this.dataManager = dataManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerData data = dataManager.getOrCreate(event.getPlayer());
        data.setLastWorldName(event.getPlayer().getWorld().getName());
        dataManager.updateEntityMapping(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
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
}
