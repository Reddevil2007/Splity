package com.splitcloud.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    private ExpenseGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payer_id")
    private Member payer;

    @Column(nullable = false, length = 120)
    private String description;

    /** Money is stored in paise as an integer, never as a floating point value. */
    @Column(nullable = false)
    private long amountPaise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SplitType splitType;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpenseSplit> splits = new ArrayList<>();

    protected Expense() {
    }

    public Expense(ExpenseGroup group, Member payer, String description, long amountPaise, SplitType splitType) {
        this.group = group;
        this.payer = payer;
        this.description = description;
        this.amountPaise = amountPaise;
        this.splitType = splitType;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void addSplit(Member member, long sharePaise) {
        splits.add(new ExpenseSplit(this, member, sharePaise));
    }

    public Long getId() {
        return id;
    }

    public ExpenseGroup getGroup() {
        return group;
    }

    public Member getPayer() {
        return payer;
    }

    public String getDescription() {
        return description;
    }

    public long getAmountPaise() {
        return amountPaise;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<ExpenseSplit> getSplits() {
        return splits;
    }
}
