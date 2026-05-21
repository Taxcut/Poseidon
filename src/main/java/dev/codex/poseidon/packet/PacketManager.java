package dev.codex.poseidon.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.EnumWrappers;
import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.check.CheckManager;
import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.data.PlayerDataManager;
import dev.codex.poseidon.processor.CombatProcessor;
import dev.codex.poseidon.processor.MovementProcessor;
import dev.codex.poseidon.processor.ServerPacketProcessor;
import dev.codex.poseidon.replay.ReplayRecorder;
import dev.codex.poseidon.transaction.TransactionManager;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class PacketManager {
    private final PoseidonPlugin plugin;
    private final PlayerDataManager dataManager;
    private final CheckManager checkManager;
    private final TransactionManager transactionManager;
    private final ReplayRecorder replayRecorder;
    private final ProtocolManager protocolManager;
    private final MovementProcessor movementProcessor;
    private final CombatProcessor combatProcessor;
    private final ServerPacketProcessor serverPacketProcessor;
    private final AtomicLong totalPacketNanos = new AtomicLong();
    private final AtomicLong packetExecutions = new AtomicLong();
    private PacketAdapter clientListener;
    private PacketAdapter serverListener;

    public PacketManager(PoseidonPlugin plugin,
                         PlayerDataManager dataManager,
                         CheckManager checkManager,
                         TransactionManager transactionManager,
                         ReplayRecorder replayRecorder) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.checkManager = checkManager;
        this.transactionManager = transactionManager;
        this.replayRecorder = replayRecorder;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.movementProcessor = new MovementProcessor();
        this.combatProcessor = new CombatProcessor();
        this.serverPacketProcessor = new ServerPacketProcessor(dataManager, replayRecorder);
    }

    public void register() {
        unregister();
        PacketType[] clientTypes = supportedTypes(
                PacketType.Play.Client.FLYING,
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.POSITION_LOOK,
                PacketType.Play.Client.LOOK,
                PacketType.Play.Client.USE_ENTITY,
                PacketType.Play.Client.WINDOW_CLICK,
                PacketType.Play.Client.BLOCK_PLACE,
                PacketType.Play.Client.CUSTOM_PAYLOAD,
                PacketType.Play.Client.UPDATE_SIGN,
                PacketType.Play.Client.TAB_COMPLETE,
                PacketType.Play.Client.TRANSACTION);

        if (clientTypes.length > 0) {
            clientListener = new PacketAdapter(plugin, ListenerPriority.NORMAL, clientTypes) {
                @Override
                public void onPacketReceiving(PacketEvent event) {
                    handleClientPacket(event);
                }
            };

            protocolManager.addPacketListener(clientListener);
        }

        PacketType[] serverTypes = supportedTypes(
                PacketType.Play.Server.ENTITY_VELOCITY,
                PacketType.Play.Server.POSITION);

        if (serverTypes.length > 0) {
            serverListener = new PacketAdapter(plugin, ListenerPriority.NORMAL, serverTypes) {
                @Override
                public void onPacketSending(PacketEvent event) {
                    serverPacketProcessor.process(event);
                }
            };

            protocolManager.addPacketListener(serverListener);
        }
    }

    public void unregister() {
        if (clientListener != null) {
            protocolManager.removePacketListener(clientListener);
            clientListener = null;
        }
        if (serverListener != null) {
            protocolManager.removePacketListener(serverListener);
            serverListener = null;
        }
    }

    private void handleClientPacket(PacketEvent event) {
        long started = System.nanoTime();
        try {
            handleClientPacketMeasured(event);
        } finally {
            totalPacketNanos.addAndGet(System.nanoTime() - started);
            packetExecutions.incrementAndGet();
        }
    }

    private void handleClientPacketMeasured(PacketEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        PlayerData data = dataManager.getOrCreate(player);
        PacketType type = event.getPacketType();
        PacketContainer packet = event.getPacket();
        long now = System.currentTimeMillis();

        if (type == PacketType.Play.Client.TRANSACTION) {
            Short action = read(packet.getShorts(), 0, null);
            if (action != null) {
                transactionManager.handleClientTransaction(data, action.shortValue(), now);
            }
            return;
        }

        PacketContext context = buildContext(player, data, type, packet, now);
        checkManager.handle(context);
        postProcess(context);
        replayRecorder.record(context);
    }

    public double averagePacketMicros() {
        long count = packetExecutions.get();
        if (count <= 0L) {
            return 0.0D;
        }
        return totalPacketNanos.get() / (double) count / 1000.0D;
    }

    private PacketContext buildContext(Player player, PlayerData data, PacketType type, PacketContainer packet, long now) {
        boolean hasPosition = type == PacketType.Play.Client.POSITION || type == PacketType.Play.Client.POSITION_LOOK;
        boolean hasRotation = type == PacketType.Play.Client.LOOK || type == PacketType.Play.Client.POSITION_LOOK;
        boolean hasGround = type == PacketType.Play.Client.FLYING
                || type == PacketType.Play.Client.POSITION
                || type == PacketType.Play.Client.POSITION_LOOK
                || type == PacketType.Play.Client.LOOK;

        double x = hasPosition ? read(packet.getDoubles(), 0, Double.valueOf(data.getX())).doubleValue() : data.getX();
        double y = hasPosition ? read(packet.getDoubles(), 1, Double.valueOf(data.getY())).doubleValue() : data.getY();
        double z = hasPosition ? read(packet.getDoubles(), 2, Double.valueOf(data.getZ())).doubleValue() : data.getZ();
        float yaw = hasRotation ? read(packet.getFloat(), 0, Float.valueOf(data.getYaw())).floatValue() : data.getYaw();
        float pitch = hasRotation ? read(packet.getFloat(), 1, Float.valueOf(data.getPitch())).floatValue() : data.getPitch();
        boolean onGround = hasGround ? read(packet.getBooleans(), 0, Boolean.valueOf(data.isOnGround())).booleanValue() : data.isOnGround();
        int entityId = type == PacketType.Play.Client.USE_ENTITY ? read(packet.getIntegers(), 0, Integer.valueOf(-1)).intValue() : -1;
        EnumWrappers.EntityUseAction useAction = type == PacketType.Play.Client.USE_ENTITY
                ? read(packet.getEntityUseActions(), 0, null)
                : null;

        return new PacketContext(player, data, type, packet, now, hasPosition, hasRotation, hasGround,
                x, y, z, yaw, pitch, onGround, entityId, useAction);
    }

    private void postProcess(PacketContext context) {
        movementProcessor.process(context);
        combatProcessor.process(context);
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

    private PacketType[] supportedTypes(PacketType... packetTypes) {
        List<PacketType> supported = new ArrayList<PacketType>(packetTypes.length);
        for (PacketType packetType : packetTypes) {
            if (packetType.isSupported()) {
                supported.add(packetType);
            } else {
                plugin.getLogger().warning("Skipping unsupported packet listener type " + packetType.name());
            }
        }
        return supported.toArray(new PacketType[supported.size()]);
    }
}
