package dev.codex.poseidon.data;

import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public final class PlayerDataTest {
    @Test
    public void transactionTimeoutCleanupRemovesOldEntries() {
        PlayerData data = new PlayerData(UUID.randomUUID(), "Test");
        data.addTransaction((short) -1, 1000L);
        data.addTransaction((short) -2, 5000L);

        data.purgeStaleTransactions(7000L, 3000L);

        Assert.assertEquals(1, data.getPendingTransactionCount());
    }

    @Test
    public void transactionTrimBoundsQueue() {
        PlayerData data = new PlayerData(UUID.randomUUID(), "Test");
        data.addTransaction((short) -1, 1000L);
        data.addTransaction((short) -2, 2000L);
        data.addTransaction((short) -3, 3000L);

        data.trimPendingTransactions(2);

        Assert.assertEquals(2, data.getPendingTransactionCount());
    }

    @Test
    public void maxViolationCapsStoredValue() {
        PlayerData data = new PlayerData(UUID.randomUUID(), "Test");

        data.addViolation("MoveA", 10.0D, 12.0D);
        data.addViolation("MoveA", 10.0D, 12.0D);

        Assert.assertEquals(12.0D, data.getViolations().get("MoveA").doubleValue(), 0.0001D);
    }
}
