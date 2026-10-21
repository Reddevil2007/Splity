package com.splitcloud.domain;

import jakarta.persistence.*;

/** One participant's share of one expense. */
@Entity
@Table(name = "expense_splits")
public class ExpenseSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id")
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false)
    private long sharePaise;

    protected ExpenseSplit() {
    }

    ExpenseSplit(Expense expense, Member member, long sharePaise) {
        this.expense = expense;
        this.member = member;
        this.sharePaise = sharePaise;
    }

    public Long getId() {
        return id;
    }

    public Expense getExpense() {
        return expense;
    }

    public Member getMember() {
        return member;
    }

    public long getSharePaise() {
        return sharePaise;
    }
}
