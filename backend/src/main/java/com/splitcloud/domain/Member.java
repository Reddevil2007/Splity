package com.splitcloud.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "group_members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    private ExpenseGroup group;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(length = 120)
    private String email;

    /** UPI virtual payment address, e.g. name@okaxis. Optional. */
    @Column(length = 80)
    private String upiId;

    protected Member() {
    }

    public Member(ExpenseGroup group, String name, String email, String upiId) {
        this.group = group;
        this.name = name;
        this.email = email;
        this.upiId = upiId;
    }

    public Long getId() {
        return id;
    }

    public ExpenseGroup getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getUpiId() {
        return upiId;
    }
}
