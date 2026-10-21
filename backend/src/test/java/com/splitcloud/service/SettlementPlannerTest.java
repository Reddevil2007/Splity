package com.splitcloud.service;

import com.splitcloud.service.SettlementPlanner.Transfer;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SettlementPlannerTest {

    /** Applies the transfers and checks every balance ends at exactly zero. */
    private static void assertSettlesEverything(Map<Long, Long> net, List<Transfer> transfers) {
        Map<Long, Long> after = new HashMap<>(net);
        for (Transfer t : transfers) {
            assertTrue(t.amountPaise() > 0);
            after.merge(t.fromMemberId(), t.amountPaise(), Long::sum);
            after.merge(t.toMemberId(), -t.amountPaise(), Long::sum);
        }
        after.values().forEach(v -> assertEquals(0L, v));
    }

    @Test
    void simpleTwoPersonDebt() {
        Map<Long, Long> net = Map.of(1L, 500L, 2L, -500L);
        List<Transfer> plan = SettlementPlanner.plan(net);
        assertEquals(List.of(new Transfer(2, 1, 500)), plan);
    }

    @Test
    void chainOfDebtsCollapses() {
        // A owes B 100, B owes C 100: the plan is just A pays C 100.
        Map<Long, Long> net = new LinkedHashMap<>();
        net.put(1L, -100L);
        net.put(2L, 0L);
        net.put(3L, 100L);
        List<Transfer> plan = SettlementPlanner.plan(net);
        assertEquals(1, plan.size());
        assertEquals(new Transfer(1, 3, 100), plan.get(0));
    }

    @Test
    void settledGroupNeedsNoPayments() {
        assertTrue(SettlementPlanner.plan(Map.of(1L, 0L, 2L, 0L)).isEmpty());
    }

    @Test
    void rejectsBalancesThatDoNotSumToZero() {
        assertThrows(IllegalStateException.class, () -> SettlementPlanner.plan(Map.of(1L, 10L, 2L, -5L)));
    }

    @Test
    void randomGroupsSettleInAtMostNMinusOnePayments() {
        Random random = new Random(42);
        for (int round = 0; round < 500; round++) {
            int n = 2 + random.nextInt(9);
            Map<Long, Long> net = new LinkedHashMap<>();
            long running = 0;
            for (long id = 1; id < n; id++) {
                long value = random.nextInt(200_001) - 100_000;
                net.put(id, value);
                running += value;
            }
            net.put((long) n, -running);

            List<Transfer> plan = SettlementPlanner.plan(net);
            assertTrue(plan.size() <= n - 1, "n=" + n + " produced " + plan.size() + " payments");
            assertSettlesEverything(net, plan);
        }
    }
}
