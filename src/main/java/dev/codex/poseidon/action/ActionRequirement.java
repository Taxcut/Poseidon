package dev.codex.poseidon.action;

import java.util.Iterator;
import java.util.Queue;

public final class ActionRequirement {
    private ActionRequirement() {
    }

    public static boolean recordAndTest(Queue<Long> recentFlags, long now, long windowMs, int requiredFlags, int maxStoredFlags) {
        recentFlags.add(Long.valueOf(now));
        long cutoff = now - Math.max(0L, windowMs);
        Iterator<Long> iterator = recentFlags.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().longValue() < cutoff) {
                iterator.remove();
            }
        }
        while (recentFlags.size() > Math.max(1, maxStoredFlags)) {
            recentFlags.poll();
        }
        return recentFlags.size() >= Math.max(1, requiredFlags);
    }
}
