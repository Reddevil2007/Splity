package com.splitcloud.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "expenses")
public class Expense {

    /** Dates are shown to users in Indian Standard Time. */
    public static final ZoneId IST = ZoneId.of("Asia/Kolkata");

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

    // The two columns below were added after the first release, so they are
    // nullable: Hibernate's ddl-auto=update cannot add NOT NULL columns to a table
    // that already has rows. The getters fall back to sensible values.

    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    private ExpenseCategory category;

    /** The day the money was spent, which the user can change. */
    private LocalDate spentOn;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpenseSplit> splits = new ArrayList<>();

    protected Expense() {
    }

    public Expense(ExpenseGroup group, Member payer, String description, long amountPaise, SplitType splitType,
                   ExpenseCategory category, LocalDate spentOn) {
        this.group = group;
        this.payer = payer;
        this.description = description;
        this.amountPaise = amountPaise;
        this.splitType = splitType;
        this.category = category;
        this.spentOn = spentOn;
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

    public ExpenseCategory getCategory() {
        return category != null ? category : ExpenseCategory.guess(description);
    }

    public LocalDate getSpentOn() {
        if (spentOn != null) {
            return spentOn;
        }
        return createdAt == null ? LocalDate.now(IST) : LocalDate.ofInstant(createdAt, IST);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCategory(ExpenseCategory category) {
        this.category = category;
    }

    public void setSpentOn(LocalDate spentOn) {
        this.spentOn = spentOn;
    }

    public List<ExpenseSplit> getSplits() {
        return splits;
    }
}
