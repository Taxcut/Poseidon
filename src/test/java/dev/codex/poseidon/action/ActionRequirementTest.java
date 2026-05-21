package dev.codex.poseidon.action;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Queue;

public final class ActionRequirementTest {
    @Test
    public void requiresMultipleRecentFlags() {
        Queue<Long> flags = new ArrayDeque<Long>();

        Assert.assertFalse(ActionRequirement.recordAndTest(flags, 1000L, 10000L, 3, 10));
        Assert.assertFalse(ActionRequirement.recordAndTest(flags, 2000L, 10000L, 3, 10));
        Assert.assertTrue(ActionRequirement.recordAndTest(flags, 3000L, 10000L, 3, 10));
    }

    @Test
    public void expiresOldFlagsAndBoundsQueue() {
        Queue<Long> flags = new ArrayDeque<Long>();

        ActionRequirement.recordAndTest(flags, 1000L, 1000L, 2, 2);
        ActionRequirement.recordAndTest(flags, 2000L, 1000L, 2, 2);
        ActionRequirement.recordAndTest(flags, 4000L, 1000L, 2, 2);

        Assert.assertEquals(1, flags.size());
    }
}
