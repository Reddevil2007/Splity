package com.splitcloud.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

/** A recorded repayment: fromMember paid toMember outside the app (cash or UPI). */
@Entity
@Table(name = "settlements")
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    private ExpenseGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_member_id")
    private Member fromMember;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_member_id")
    private Member toMember;

    @Column(nullable = false)
    private long amountPaise;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** The day the repayment was made; editable, nullable for rows created before it existed. */
    private LocalDate paidOn;

    protected Settlement() {
    }

    public Settlement(ExpenseGroup group, Member fromMember, Member toMember, long amountPaise, LocalDate paidOn) {
        this.group = group;
        this.fromMember = fromMember;
        this.toMember = toMember;
        this.amountPaise = amountPaise;
        this.paidOn = paidOn;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public ExpenseGroup getGroup() {
        return group;
    }

    public Member getFromMember() {
        return fromMember;
    }

    public Member getToMember() {
        return toMember;
    }

    public long getAmountPaise() {
        return amountPaise;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public LocalDate getPaidOn() {
        if (paidOn != null) {
            return paidOn;
        }
        return createdAt == null ? LocalDate.now(Expense.IST) : LocalDate.ofInstant(createdAt, Expense.IST);
    }

    public void setPaidOn(LocalDate paidOn) {
        this.paidOn = paidOn;
    }
}
