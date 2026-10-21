package com.splitcloud.service;

import com.splitcloud.domain.SplitType;
import com.splitcloud.service.SplitCalculator.Participant;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SplitCalculatorTest {

    private static Participant p(long id, String value) {
        return new Participant(id, value == null ? null : new BigDecimal(value));
    }

    private static long sum(Map<Long, Long> shares) {
        return shares.values().stream().mapToLong(Long::longValue).sum();
    }

    @Test
    void equalSplitHandsOutRemainderSoNothingIsLost() {
        Map<Long, Long> shares = SplitCalculator.split(10_000, SplitType.EQUAL,
                List.of(p(1, null), p(2, null), p(3, null)));
        assertEquals(List.of(3334L, 3333L, 3333L), List.copyOf(shares.values()));
        assertEquals(10_000, sum(shares));
    }

    @Test
    void sharesSplitFollowsWeights() {
        Map<Long, Long> shares = SplitCalculator.split(360_000, SplitType.SHARES,
                List.of(p(1, "2"), p(2, "1"), p(3, "2"), p(4, "1")));
        assertEquals(120_000L, shares.get(1L));
        assertEquals(60_000L, shares.get(2L));
        assertEquals(360_000, sum(shares));
    }

    @Test
    void percentSplitMustAddUpTo100() {
        Map<Long, Long> shares = SplitCalculator.split(99_999, SplitType.PERCENT,
                List.of(p(1, "50"), p(2, "25"), p(3, "25")));
        assertEquals(99_999, sum(shares));

        assertThrows(BadRequestException.class, () -> SplitCalculator.split(10_000, SplitType.PERCENT,
                List.of(p(1, "50"), p(2, "40"))));
    }

    @Test
    void exactSplitMustMatchTotal() {
        Map<Long, Long> shares = SplitCalculator.split(5_000, SplitType.EXACT,
                List.of(p(1, "1000"), p(2, "4000")));
        assertEquals(4_000L, shares.get(2L));

        assertThrows(BadRequestException.class, () -> SplitCalculator.split(5_000, SplitType.EXACT,
                List.of(p(1, "1000"), p(2, "1000"))));
    }

    @Test
    void rejectsDuplicateParticipantsAndNonPositiveAmounts() {
        assertThrows(BadRequestException.class, () -> SplitCalculator.split(1_000, SplitType.EQUAL,
                List.of(p(1, null), p(1, null))));
        assertThrows(BadRequestException.class, () -> SplitCalculator.split(0, SplitType.EQUAL,
                List.of(p(1, null))));
        assertThrows(BadRequestException.class, () -> SplitCalculator.split(1_000, SplitType.SHARES,
                List.of(p(1, "0"), p(2, "1"))));
    }

    @Test
    void everySplitTypeAlwaysSumsToTotal() {
        for (long total = 1; total <= 2_000; total += 37) {
            assertEquals(total, sum(SplitCalculator.split(total, SplitType.EQUAL,
                    List.of(p(1, null), p(2, null), p(3, null), p(4, null), p(5, null), p(6, null), p(7, null)))));
            assertEquals(total, sum(SplitCalculator.split(total, SplitType.SHARES,
                    List.of(p(1, "3"), p(2, "7"), p(3, "11")))));
            assertEquals(total, sum(SplitCalculator.split(total, SplitType.PERCENT,
                    List.of(p(1, "33.33"), p(2, "33.33"), p(3, "33.34")))));
        }
    }
}
