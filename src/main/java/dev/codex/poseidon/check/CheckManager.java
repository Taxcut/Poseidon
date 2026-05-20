package dev.codex.poseidon.check;

import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.impl.BadPacketsACheck;
import dev.codex.poseidon.check.impl.BadPacketsBCheck;
import dev.codex.poseidon.check.impl.BadPacketsCCheck;
import dev.codex.poseidon.check.impl.AutoClickerACheck;
import dev.codex.poseidon.check.impl.InventoryACheck;
import dev.codex.poseidon.check.impl.LatencyAbuseACheck;
import dev.codex.poseidon.check.impl.MoveACheck;
import dev.codex.poseidon.check.impl.ModelACheck;
import dev.codex.poseidon.check.impl.ReachACheck;
import dev.codex.poseidon.check.impl.SimulationACheck;
import dev.codex.poseidon.check.impl.TimerACheck;
import dev.codex.poseidon.check.impl.WorldInteractionACheck;
import dev.codex.poseidon.packet.PacketContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CheckManager {
    private final List<Check> checks = new ArrayList<Check>();

    public CheckManager(PoseidonPlugin plugin, AlertManager alertManager) {
        checks.add(new BadPacketsACheck(plugin, alertManager));
        checks.add(new BadPacketsBCheck(plugin, alertManager));
        checks.add(new BadPacketsCCheck(plugin, alertManager));
        checks.add(new TimerACheck(plugin, alertManager));
        checks.add(new LatencyAbuseACheck(plugin, alertManager));
        checks.add(new MoveACheck(plugin, alertManager));
        checks.add(new SimulationACheck(plugin, alertManager));
        checks.add(new ModelACheck(plugin, alertManager));
        checks.add(new ReachACheck(plugin, alertManager));
        checks.add(new AutoClickerACheck(plugin, alertManager));
        checks.add(new InventoryACheck(plugin, alertManager));
        checks.add(new WorldInteractionACheck(plugin, alertManager));
    }

    public void handle(PacketContext context) {
        for (Check check : checks) {
            if (check.isEnabled()) {
                check.handle(context);
            }
        }
    }

    public int size() {
        return checks.size();
    }

    public List<Check> getChecks() {
        return Collections.unmodifiableList(checks);
    }
}
