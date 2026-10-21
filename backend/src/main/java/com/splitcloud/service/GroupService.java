package com.splitcloud.service;

import com.splitcloud.domain.*;
import com.splitcloud.repo.ExpenseRepository;
import com.splitcloud.repo.GroupRepository;
import com.splitcloud.repo.MemberRepository;
import com.splitcloud.repo.SettlementRepository;
import com.splitcloud.web.Dtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class GroupService {

    private final GroupRepository groups;
    private final MemberRepository members;
    private final ExpenseRepository expenses;
    private final SettlementRepository settlements;

    /** Constructor injection: Spring supplies the repositories (dependency injection). */
    public GroupService(GroupRepository groups, MemberRepository members,
                        ExpenseRepository expenses, SettlementRepository settlements) {
        this.groups = groups;
        this.members = members;
        this.expenses = expenses;
        this.settlements = settlements;
    }

    // ---------- groups and members ----------

    @Transactional(readOnly = true)
    public List<GroupSummary> listGroups() {
        return groups.findAllByOrderByCreatedAtDesc().stream().map(this::summary).toList();
    }

    public GroupSummary createGroup(CreateGroupRequest request) {
        ExpenseGroup group = groups.save(new ExpenseGroup(request.name().trim()));
        return summary(group);
    }

    @Transactional(readOnly = true)
    public GroupDetail getGroup(long groupId) {
        ExpenseGroup group = requireGroup(groupId);
        List<MemberView> memberViews = members.findByGroupIdOrderByIdAsc(groupId).stream()
                .map(m -> new MemberView(m.getId(), m.getName(), m.getEmail(), m.getUpiId()))
                .toList();
        return new GroupDetail(group.getId(), group.getName(), group.getCreatedAt(),
                expenses.totalSpentByGroupId(groupId), memberViews);
    }

    public MemberView addMember(long groupId, AddMemberRequest request) {
        ExpenseGroup group = requireGroup(groupId);
        Member member = members.save(new Member(group, request.name().trim(),
                blankToNull(request.email()), blankToNull(request.upiId())));
        return new MemberView(member.getId(), member.getName(), member.getEmail(), member.getUpiId());
    }

    // ---------- expenses ----------

    @Transactional(readOnly = true)
    public List<ExpenseView> listExpenses(long groupId) {
        requireGroup(groupId);
        return expenses.findWithSplitsByGroupId(groupId).stream().map(GroupService::toView).toList();
    }

    public ExpenseView addExpense(long groupId, CreateExpenseRequest request) {
        ExpenseGroup group = requireGroup(groupId);
        Map<Long, Member> groupMembers = membersById(groupId);

        Member payer = groupMembers.get(request.payerId());
        if (payer == null) {
            throw new BadRequestException("The payer is not a member of this group");
        }

        List<SplitCalculator.Participant> participants = request.participants().stream()
                .map(p -> {
                    if (!groupMembers.containsKey(p.memberId())) {
                        throw new BadRequestException("Participant " + p.memberId() + " is not in this group");
                    }
                    return new SplitCalculator.Participant(p.memberId(), p.value());
                })
                .toList();

        Map<Long, Long> shares = SplitCalculator.split(request.amountPaise(), request.splitType(), participants);

        Expense expense = new Expense(group, payer, request.description().trim(),
                request.amountPaise(), request.splitType());
        shares.forEach((memberId, share) -> expense.addSplit(groupMembers.get(memberId), share));
        return toView(expenses.save(expense));
    }

    public void deleteExpense(long groupId, long expenseId) {
        Expense expense = expenses.findById(expenseId)
                .filter(e -> e.getGroup().getId().equals(groupId))
                .orElseThrow(() -> new NotFoundException("Expense " + expenseId + " not found in this group"));
        expenses.delete(expense);
    }

    // ---------- balances and settlement ----------

    @Transactional(readOnly = true)
    public List<BalanceView> balances(long groupId) {
        requireGroup(groupId);
        List<Member> groupMembers = members.findByGroupIdOrderByIdAsc(groupId);

        Map<Long, Long> paid = new HashMap<>();
        Map<Long, Long> share = new HashMap<>();
        Map<Long, Long> net = new HashMap<>();
        groupMembers.forEach(m -> {
            paid.put(m.getId(), 0L);
            share.put(m.getId(), 0L);
            net.put(m.getId(), 0L);
        });

        for (Expense e : expenses.findWithSplitsByGroupId(groupId)) {
            paid.merge(e.getPayer().getId(), e.getAmountPaise(), Long::sum);
            net.merge(e.getPayer().getId(), e.getAmountPaise(), Long::sum);
            for (ExpenseSplit s : e.getSplits()) {
                share.merge(s.getMember().getId(), s.getSharePaise(), Long::sum);
                net.merge(s.getMember().getId(), -s.getSharePaise(), Long::sum);
            }
        }
        // A repayment moves the payer's balance up and the receiver's balance down.
        for (Settlement s : settlements.findWithMembersByGroupId(groupId)) {
            net.merge(s.getFromMember().getId(), s.getAmountPaise(), Long::sum);
            net.merge(s.getToMember().getId(), -s.getAmountPaise(), Long::sum);
        }

        return groupMembers.stream()
                .map(m -> new BalanceView(m.getId(), m.getName(), paid.get(m.getId()),
                        share.get(m.getId()), net.get(m.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransferView> settlementPlan(long groupId) {
        ExpenseGroup group = requireGroup(groupId);
        Map<Long, Member> byId = membersById(groupId);
        Map<Long, Long> net = new LinkedHashMap<>();
        balances(groupId).forEach(b -> net.put(b.memberId(), b.netPaise()));

        return SettlementPlanner.plan(net).stream()
                .map(t -> {
                    Member from = byId.get(t.fromMemberId());
                    Member to = byId.get(t.toMemberId());
                    String link = UpiLinkBuilder.build(to.getUpiId(), to.getName(), t.amountPaise(),
                            "SplitCloud - " + group.getName());
                    return new TransferView(from.getId(), from.getName(), to.getId(), to.getName(),
                            t.amountPaise(), link);
                })
                .toList();
    }

    public SettlementView recordSettlement(long groupId, RecordSettlementRequest request) {
        ExpenseGroup group = requireGroup(groupId);
        Map<Long, Member> byId = membersById(groupId);
        Member from = byId.get(request.fromMemberId());
        Member to = byId.get(request.toMemberId());
        if (from == null || to == null) {
            throw new BadRequestException("Both people must be members of this group");
        }
        if (from.getId().equals(to.getId())) {
            throw new BadRequestException("A member cannot pay themselves");
        }
        Settlement saved = settlements.save(new Settlement(group, from, to, request.amountPaise()));
        return toView(saved);
    }

    @Transactional(readOnly = true)
    public List<SettlementView> listSettlements(long groupId) {
        requireGroup(groupId);
        return settlements.findWithMembersByGroupId(groupId).stream().map(GroupService::toView).toList();
    }

    /** Everyone who currently owes money, across all groups. Feeds reminder jobs. */
    @Transactional(readOnly = true)
    public List<ReminderView> pendingReminders() {
        List<ReminderView> result = new ArrayList<>();
        for (ExpenseGroup group : groups.findAllByOrderByCreatedAtDesc()) {
            Map<Long, Member> byId = membersById(group.getId());
            for (BalanceView b : balances(group.getId())) {
                if (b.netPaise() < 0) {
                    Member m = byId.get(b.memberId());
                    result.add(new ReminderView(group.getId(), group.getName(), m.getId(), m.getName(),
                            m.getEmail(), -b.netPaise()));
                }
            }
        }
        return result;
    }

    // ---------- demo data ----------

    /** Creates a ready-made "Goa Trip" group so the live demo does not start from an empty screen. */
    public GroupSummary seedDemo() {
        ExpenseGroup group = groups.save(new ExpenseGroup("Goa Trip"));
        Member aarav = members.save(new Member(group, "Aarav", "aarav@example.com", "aarav@okaxis"));
        Member diya = members.save(new Member(group, "Diya", "diya@example.com", "diya@oksbi"));
        Member kabir = members.save(new Member(group, "Kabir", "kabir@example.com", "kabir@ybl"));
        Member meera = members.save(new Member(group, "Meera", "meera@example.com", "meera@okhdfcbank"));

        List<Long> everyone = List.of(aarav.getId(), diya.getId(), kabir.getId(), meera.getId());
        demoExpense(group, aarav, "Hotel booking", 1_200_000, SplitType.EQUAL, everyone, null);
        demoExpense(group, diya, "Scooter rental", 240_000, SplitType.EQUAL, everyone, null);
        demoExpense(group, kabir, "Beach shack dinner", 360_000, SplitType.SHARES,
                List.of(aarav.getId(), diya.getId(), kabir.getId(), meera.getId()), List.of(2, 1, 2, 1));
        demoExpense(group, meera, "Cab to airport", 150_000, SplitType.EQUAL,
                List.of(diya.getId(), kabir.getId(), meera.getId()), null);
        return summary(group);
    }

    private void demoExpense(ExpenseGroup group, Member payer, String description, long amountPaise,
                             SplitType type, List<Long> memberIds, List<Integer> weights) {
        List<ParticipantRequest> participants = new ArrayList<>();
        for (int i = 0; i < memberIds.size(); i++) {
            BigDecimal value = weights == null ? null : BigDecimal.valueOf(weights.get(i));
            participants.add(new ParticipantRequest(memberIds.get(i), value));
        }
        addExpense(group.getId(), new CreateExpenseRequest(description, amountPaise, payer.getId(), type,
                participants));
    }

    // ---------- helpers ----------

    private ExpenseGroup requireGroup(long groupId) {
        return groups.findById(groupId).orElseThrow(() -> new NotFoundException("Group " + groupId + " not found"));
    }

    private Map<Long, Member> membersById(long groupId) {
        return members.findByGroupIdOrderByIdAsc(groupId).stream()
                .collect(Collectors.toMap(Member::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    private GroupSummary summary(ExpenseGroup g) {
        return new GroupSummary(g.getId(), g.getName(), members.countByGroupId(g.getId()),
                expenses.totalSpentByGroupId(g.getId()), g.getCreatedAt());
    }

    private static ExpenseView toView(Expense e) {
        List<SplitView> splits = e.getSplits().stream()
                .sorted(Comparator.comparing(s -> s.getMember().getId()))
                .map(s -> new SplitView(s.getMember().getId(), s.getMember().getName(), s.getSharePaise()))
                .toList();
        return new ExpenseView(e.getId(), e.getDescription(), e.getAmountPaise(), e.getSplitType(),
                e.getPayer().getId(), e.getPayer().getName(), e.getCreatedAt(), splits);
    }

    private static SettlementView toView(Settlement s) {
        return new SettlementView(s.getId(), s.getFromMember().getId(), s.getFromMember().getName(),
                s.getToMember().getId(), s.getToMember().getName(), s.getAmountPaise(), s.getCreatedAt());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
