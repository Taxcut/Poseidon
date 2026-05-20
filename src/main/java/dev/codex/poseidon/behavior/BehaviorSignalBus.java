package dev.codex.poseidon.behavior;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class BehaviorSignalBus {
    private final List<BehaviorSignalListener> listeners = new CopyOnWriteArrayList<BehaviorSignalListener>();

    public void register(BehaviorSignalListener listener) {
        listeners.add(listener);
    }

    public void publish(BehaviorSignal signal) {
        for (BehaviorSignalListener listener : listeners) {
            listener.onSignal(signal);
        }
    }
}
