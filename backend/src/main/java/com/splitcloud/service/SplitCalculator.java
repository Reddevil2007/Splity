package com.splitcloud.service;

import com.splitcloud.domain.SplitType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Turns "total amount + split rule + participants" into an exact amount in paise
 * per participant. Pure logic, no database, so it is unit tested directly.
 *
 * Every split is guaranteed to add up to the total exactly. Remainders from
 * rounding are handed out with the largest-remainder method, so 100.00 split
 * three ways becomes 33.34 + 33.33 + 33.33 and no paisa is lost.
 */
public final class SplitCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private SplitCalculator() {
    }

    /** One participant and the number that goes with the chosen split type. */
    public record Participant(long memberId, BigDecimal value) {
    }

    /** @return memberId to share in paise, in the same order as the input */
    public static Map<Long, Long> split(long totalPaise, SplitType type, List<Participant> participants) {
        if (totalPaise <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }
        if (participants == null || participants.isEmpty()) {
            throw new BadRequestException("Pick at least one participant");
        }
        Set<Long> seen = new HashSet<>();
        for (Participant p : participants) {
            if (!seen.add(p.memberId())) {
                throw new BadRequestException("A member appears twice in the split");
            }
        }

        return switch (type) {
            case EQUAL -> weighted(totalPaise, participants.stream()
                    .map(p -> new Participant(p.memberId(), BigDecimal.ONE)).toList());
            case SHARES -> {
                requirePositiveValues(participants, "Each share count must be greater than zero");
                yield weighted(totalPaise, participants);
            }
            case PERCENT -> {
                requirePositiveValues(participants, "Each percentage must be greater than zero");
                BigDecimal sum = sum(participants);
                if (sum.subtract(HUNDRED).abs().compareTo(new BigDecimal("0.01")) > 0) {
                    throw new BadRequestException("Percentages must add up to 100 (they add up to "
                            + sum.stripTrailingZeros().toPlainString() + ")");
                }
                yield weighted(totalPaise, participants);
            }
            case EXACT -> exact(totalPaise, participants);
        };
    }

    private static Map<Long, Long> exact(long totalPaise, List<Participant> participants) {
        Map<Long, Long> result = new LinkedHashMap<>();
        long sum = 0;
        for (Participant p : participants) {
            if (p.value() == null || p.value().signum() < 0 || p.value().stripTrailingZeros().scale() > 0) {
                throw new BadRequestException("Exact amounts must be whole paise, zero or more");
            }
            long paise = p.value().longValueExact();
            result.put(p.memberId(), paise);
            sum += paise;
        }
        if (sum != totalPaise) {
            throw new BadRequestException("Exact amounts add up to " + sum + " paise but the expense is "
                    + totalPaise + " paise");
        }
        return result;
    }

    /** Largest-remainder allocation of totalPaise in proportion to each participant's weight. */
    private static Map<Long, Long> weighted(long totalPaise, List<Participant> participants) {
        BigDecimal totalWeight = sum(participants);
        BigDecimal total = BigDecimal.valueOf(totalPaise);

        long[] floors = new long[participants.size()];
        BigDecimal[] remainders = new BigDecimal[participants.size()];
        long allocated = 0;
        for (int i = 0; i < participants.size(); i++) {
            BigDecimal exactShare = total.multiply(participants.get(i).value())
                    .divide(totalWeight, 10, RoundingMode.HALF_EVEN);
            BigDecimal floor = exactShare.setScale(0, RoundingMode.FLOOR);
            floors[i] = floor.longValueExact();
            remainders[i] = exactShare.subtract(floor);
            allocated += floors[i];
        }

        // Give the leftover paise to the largest remainders; ties go to the earlier participant.
        Integer[] order = new Integer[participants.size()];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> {
            int cmp = remainders[b].compareTo(remainders[a]);
            return cmp != 0 ? cmp : Integer.compare(a, b);
        });
        long leftover = totalPaise - allocated;
        for (int k = 0; k < leftover; k++) {
            floors[order[k % order.length]]++;
        }

        Map<Long, Long> result = new LinkedHashMap<>();
        for (int i = 0; i < participants.size(); i++) {
            result.put(participants.get(i).memberId(), floors[i]);
        }
        return result;
    }

    private static void requirePositiveValues(List<Participant> participants, String message) {
        for (Participant p : participants) {
            if (p.value() == null || p.value().signum() <= 0) {
                throw new BadRequestException(message);
            }
        }
    }

    private static BigDecimal sum(List<Participant> participants) {
        return participants.stream().map(Participant::value).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
