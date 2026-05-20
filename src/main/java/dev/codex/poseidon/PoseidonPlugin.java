package dev.codex.poseidon;

import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.behavior.BehaviorSignalBus;
import dev.codex.poseidon.check.CheckManager;
import dev.codex.poseidon.cloud.CrossInstanceReputationManager;
import dev.codex.poseidon.cloud.CloudSyncManager;
import dev.codex.poseidon.command.PoseidonCommand;
import dev.codex.poseidon.config.ConfigValidator;
import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.data.PlayerDataManager;
import dev.codex.poseidon.listener.InventoryLifecycleListener;
import dev.codex.poseidon.listener.PlayerLifecycleListener;
import dev.codex.poseidon.mitigation.MitigationManager;
import dev.codex.poseidon.model.OnlineBehaviorModel;
import dev.codex.poseidon.packet.PacketManager;
import dev.codex.poseidon.processor.RuntimeStateSampler;
import dev.codex.poseidon.replay.ReplayRecorder;
import dev.codex.poseidon.server.TpsTracker;
import dev.codex.poseidon.transaction.TransactionManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class PoseidonPlugin extends JavaPlugin {
    private PlayerDataManager dataManager;
    private BehaviorSignalBus behaviorSignalBus;
    private AlertManager alertManager;
    private CloudSyncManager cloudSyncManager;
    private CrossInstanceReputationManager crossInstanceReputationManager;
    private MitigationManager mitigationManager;
    private OnlineBehaviorModel onlineBehaviorModel;
    private CheckManager checkManager;
    private PacketManager packetManager;
    private TransactionManager transactionManager;
    private RuntimeStateSampler runtimeStateSampler;
    private ReplayRecorder replayRecorder;
    private TpsTracker tpsTracker;
    private BukkitTask violationDecayTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        dataManager = new PlayerDataManager();
        behaviorSignalBus = new BehaviorSignalBus();
        alertManager = new AlertManager(this, behaviorSignalBus);
        cloudSyncManager = new CloudSyncManager(this);
        behaviorSignalBus.register(cloudSyncManager);
        crossInstanceReputationManager = new CrossInstanceReputationManager(this, dataManager);
        behaviorSignalBus.register(crossInstanceReputationManager);
        onlineBehaviorModel = new OnlineBehaviorModel(this, dataManager);
        behaviorSignalBus.register(onlineBehaviorModel);
        mitigationManager = new MitigationManager(this, dataManager);
        behaviorSignalBus.register(mitigationManager);
        checkManager = new CheckManager(this, alertManager);
        ConfigValidator.validate(this, checkManager);
        runtimeStateSampler = new RuntimeStateSampler(this, dataManager);
        replayRecorder = new ReplayRecorder(this);
        tpsTracker = new TpsTracker(this);
        transactionManager = new TransactionManager(this, dataManager);
        packetManager = new PacketManager(this, dataManager, checkManager, transactionManager, replayRecorder);

        Bukkit.getPluginManager().registerEvents(new PlayerLifecycleListener(dataManager), this);
        Bukkit.getPluginManager().registerEvents(new InventoryLifecycleListener(dataManager), this);
        Bukkit.getPluginManager().registerEvents(mitigationManager, this);
        PoseidonCommand command = new PoseidonCommand(this, dataManager, alertManager);
        registerCommand("poseidon", command);
        registerCommand("alerts", command);

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = dataManager.getOrCreate(player);
            data.setLastWorldName(player.getWorld().getName());
        }

        packetManager.register();
        transactionManager.start();
        cloudSyncManager.start();
        crossInstanceReputationManager.start();
        runtimeStateSampler.start();
        tpsTracker.start();
        onlineBehaviorModel.start();
        mitigationManager.start();
        startViolationDecay();

        getLogger().info("Poseidon enabled with " + checkManager.size() + " checks.");
    }

    @Override
    public void onDisable() {
        if (transactionManager != null) {
            transactionManager.stop();
        }
        if (cloudSyncManager != null) {
            cloudSyncManager.stop();
        }
        if (crossInstanceReputationManager != null) {
            crossInstanceReputationManager.stop();
        }
        if (runtimeStateSampler != null) {
            runtimeStateSampler.stop();
        }
        if (replayRecorder != null) {
            replayRecorder.stop();
        }
        if (tpsTracker != null) {
            tpsTracker.stop();
        }
        if (onlineBehaviorModel != null) {
            onlineBehaviorModel.stop();
        }
        if (mitigationManager != null) {
            mitigationManager.stop();
        }
        if (violationDecayTask != null) {
            violationDecayTask.cancel();
            violationDecayTask = null;
        }
        if (packetManager != null) {
            packetManager.unregister();
        }
        if (alertManager != null) {
            alertManager.stop();
        }
        if (dataManager != null) {
            dataManager.clear();
        }
    }

    public PlayerDataManager getDataManager() {
        return dataManager;
    }

    public AlertManager getAlertManager() {
        return alertManager;
    }

    public BehaviorSignalBus getBehaviorSignalBus() {
        return behaviorSignalBus;
    }

    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    public CloudSyncManager getCloudSyncManager() {
        return cloudSyncManager;
    }

    public CrossInstanceReputationManager getCrossInstanceReputationManager() {
        return crossInstanceReputationManager;
    }

    public MitigationManager getMitigationManager() {
        return mitigationManager;
    }

    public CheckManager getCheckManager() {
        return checkManager;
    }

    public TpsTracker getTpsTracker() {
        return tpsTracker;
    }

    public PacketManager getPacketManager() {
        return packetManager;
    }

    public ReplayRecorder getReplayRecorder() {
        return replayRecorder;
    }

    public void reloadPoseidon() {
        reloadConfig();
        if (alertManager != null) {
            alertManager.reload();
        }
        if (checkManager != null) {
            checkManager.reload();
            ConfigValidator.validate(this, checkManager);
        }
        if (runtimeStateSampler != null) {
            runtimeStateSampler.start();
        }
        if (replayRecorder != null) {
            replayRecorder.reload();
        }
        if (transactionManager != null) {
            transactionManager.start();
        }
        if (cloudSyncManager != null) {
            cloudSyncManager.start();
        }
        if (crossInstanceReputationManager != null) {
            crossInstanceReputationManager.start();
        }
        if (onlineBehaviorModel != null) {
            onlineBehaviorModel.start();
        }
        if (mitigationManager != null) {
            mitigationManager.start();
        }
        if (violationDecayTask != null) {
            violationDecayTask.cancel();
            violationDecayTask = null;
        }
        startViolationDecay();
    }

    public boolean applyProfile(String profile) {
        if ("monitor".equalsIgnoreCase(profile)) {
            getConfig().set("mitigations.mode", "monitor");
            getConfig().set("settings.verbose-default", Boolean.FALSE);
            setAllPunishments(false);
        } else if ("staff-test".equalsIgnoreCase(profile) || "stafftest".equalsIgnoreCase(profile)) {
            getConfig().set("mitigations.mode", "staff-test");
            getConfig().set("settings.verbose-default", Boolean.TRUE);
            setAllPunishments(false);
        } else if ("production".equalsIgnoreCase(profile)) {
            getConfig().set("mitigations.mode", "production");
            getConfig().set("settings.verbose-default", Boolean.FALSE);
            setStablePunishmentsOnly();
        } else {
            return false;
        }
        saveConfig();
        reloadPoseidon();
        return true;
    }

    private void setAllPunishments(boolean enabled) {
        if (checkManager == null) {
            return;
        }
        for (dev.codex.poseidon.check.Check check : checkManager.getChecks()) {
            getConfig().set("checks." + check.getName() + ".punish", Boolean.valueOf(enabled));
        }
    }

    private void setStablePunishmentsOnly() {
        if (checkManager == null) {
            return;
        }
        for (dev.codex.poseidon.check.Check check : checkManager.getChecks()) {
            boolean stable = !check.isExperimental()
                    && (check.getCategory() == dev.codex.poseidon.check.CheckCategory.PACKET
                    || check.getName().equalsIgnoreCase("TimerA")
                    || check.getName().equalsIgnoreCase("MoveA"));
            getConfig().set("checks." + check.getName() + ".punish", Boolean.valueOf(stable));
        }
    }

    private void startViolationDecay() {
        if (!getConfig().getBoolean("settings.violation-decay.enabled", true)) {
            return;
        }

        long interval = Math.max(20L, getConfig().getLong("settings.violation-decay.interval-ticks", 20L));
        final double amount = Math.max(0.0D, getConfig().getDouble("settings.violation-decay.amount", 0.15D));
        violationDecayTask = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                for (PlayerData data : dataManager.all()) {
                    checkManager.decayViolations(data, amount);
                }
            }
        }, interval, interval);
    }

    private void registerCommand(String name, PoseidonCommand command) {
        PluginCommand pluginCommand = getCommand(name);
        if (pluginCommand == null) {
            getLogger().warning("Command '" + name + "' is missing from plugin.yml.");
            return;
        }
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);
    }
}
