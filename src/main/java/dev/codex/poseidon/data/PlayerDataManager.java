package dev.codex.poseidon.data;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PlayerDataManager {
    private final ConcurrentMap<UUID, PlayerData> dataMap = new ConcurrentHashMap<UUID, PlayerData>();
    private final ConcurrentMap<Integer, UUID> entityIdMap = new ConcurrentHashMap<Integer, UUID>();

    public PlayerData getOrCreate(Player player) {
        PlayerData existing = dataMap.get(player.getUniqueId());
        if (existing != null) {
            return existing;
        }

        PlayerData created = new PlayerData(player.getUniqueId(), player.getName());
        PlayerData previous = dataMap.putIfAbsent(player.getUniqueId(), created);
        return previous == null ? created : previous;
    }

    public PlayerData get(UUID uuid) {
        return dataMap.get(uuid);
    }

    public PlayerData getByEntityId(int entityId) {
        UUID uuid = entityIdMap.get(Integer.valueOf(entityId));
        return uuid == null ? null : dataMap.get(uuid);
    }

    public void updateEntityMapping(Player player) {
        entityIdMap.put(Integer.valueOf(player.getEntityId()), player.getUniqueId());
    }

    public void remove(Player player) {
        dataMap.remove(player.getUniqueId());
        entityIdMap.remove(Integer.valueOf(player.getEntityId()));
    }

    public Collection<PlayerData> all() {
        return Collections.unmodifiableCollection(dataMap.values());
    }

    public void clear() {
        dataMap.clear();
        entityIdMap.clear();
    }
}
