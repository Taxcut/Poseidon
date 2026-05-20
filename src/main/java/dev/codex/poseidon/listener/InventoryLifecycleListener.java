package dev.codex.poseidon.listener;

import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.data.PlayerDataManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

public final class InventoryLifecycleListener implements Listener {
    private final PlayerDataManager dataManager;

    public InventoryLifecycleListener(PlayerDataManager dataManager) {
        this.dataManager = dataManager;
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player) {
            PlayerData data = dataManager.getOrCreate((Player) event.getPlayer());
            data.setInventoryOpen(true, System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            PlayerData data = dataManager.getOrCreate((Player) event.getPlayer());
            data.setInventoryOpen(false, System.currentTimeMillis());
        }
    }
}
