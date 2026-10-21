package com.splitcloud.service;

import java.util.*;

/**
 * Debt simplification ("minimum cash flow").
 *
 * Input: each member's net position in paise (positive = is owed money,
 * negative = owes money). The values always sum to zero.
 *
 * Greedy step: match the member who is owed the most with the member who owes
 * the most, settle the smaller of the two amounts, repeat. Each step clears at
 * least one member, so a group of n members settles in at most n - 1 payments,
 * instead of up to n(n-1)/2 pairwise debts.
 *
 * The true minimum number of payments is an NP-hard problem; this greedy bound
 * is what production expense apps use in practice.
 */
public final class SettlementPlanner {

    private SettlementPlanner() {
    }

    public record Transfer(long fromMemberId, long toMemberId, long amountPaise) {
    }

    private record Position(long memberId, long amount) {
    }

    public static List<Transfer> plan(Map<Long, Long> netByMember) {
        long total = netByMember.values().stream().mapToLong(Long::longValue).sum();
        if (total != 0) {
            throw new IllegalStateException("Net balances must sum to zero but sum to " + total);
        }

        // Largest amount first; ties broken by member id so the plan is deterministic.
        Comparator<Position> largestFirst = Comparator.comparingLong(Position::amount).reversed()
                .thenComparingLong(Position::memberId);
        PriorityQueue<Position> creditors = new PriorityQueue<>(largestFirst);
        PriorityQueue<Position> debtors = new PriorityQueue<>(largestFirst);

        netByMember.forEach((memberId, net) -> {
            if (net > 0) {
                creditors.add(new Position(memberId, net));
            } else if (net < 0) {
                debtors.add(new Position(memberId, -net));
            }
        });

        List<Transfer> transfers = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Position creditor = creditors.poll();
            Position debtor = debtors.poll();
            long amount = Math.min(creditor.amount(), debtor.amount());
            transfers.add(new Transfer(debtor.memberId(), creditor.memberId(), amount));

            if (creditor.amount() > amount) {
                creditors.add(new Position(creditor.memberId(), creditor.amount() - amount));
            }
            if (debtor.amount() > amount) {
                debtors.add(new Position(debtor.memberId(), debtor.amount() - amount));
            }
        }
        return transfers;
    }
}
