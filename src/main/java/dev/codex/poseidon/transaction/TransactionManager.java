package dev.codex.poseidon.transaction;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.data.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public final class TransactionManager {
    private final PoseidonPlugin plugin;
    private final PlayerDataManager dataManager;
    private final ProtocolManager protocolManager;
    private BukkitTask task;

    public TransactionManager(PoseidonPlugin plugin, PlayerDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    public void start() {
        stop();
        if (!PacketType.Play.Server.TRANSACTION.isSupported() || !PacketType.Play.Client.TRANSACTION.isSupported()) {
            plugin.getLogger().warning("Transaction packets are unsupported on this ProtocolLib/runtime; latency tracking is disabled.");
            return;
        }
        long interval = Math.max(5L, plugin.getConfig().getLong("settings.transaction-interval-ticks", 20L));
        task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                tick();
            }
        }, interval, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void handleClientTransaction(PlayerData data, short action, long timestamp) {
        data.confirmTransaction(action, timestamp);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        long staleMs = plugin.getConfig().getLong("settings.stale-transaction-ms", 10000L);
        int maxPending = plugin.getConfig().getInt("settings.max-pending-transactions", 20);
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = dataManager.getOrCreate(player);
            data.purgeStaleTransactions(now, staleMs);
            data.trimPendingTransactions(maxPending);
            sendTransaction(player, data, now);
        }
    }

    private void sendTransaction(Player player, PlayerData data, long timestamp) {
        short id = data.nextTransactionId();
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.TRANSACTION);

        try {
            packet.getIntegers().write(0, Integer.valueOf(0));
            packet.getShorts().write(0, Short.valueOf(id));
            packet.getBooleans().write(0, Boolean.FALSE);

            protocolManager.sendServerPacket(player, packet);
            data.addTransaction(id, timestamp);
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to send transaction to " + player.getName() + ": " + exception.getMessage());
        }
    }
}
