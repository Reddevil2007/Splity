package com.splitcloud.repo;

import com.splitcloud.domain.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    /**
     * Loads expenses together with their payer and splits in ONE query.
     * Without the JOIN FETCH, touching each expense's splits would issue one
     * extra query per expense: the N+1 query problem.
     */
    @Query("""
            select distinct e from Expense e
            join fetch e.payer
            left join fetch e.splits s
            left join fetch s.member
            where e.group.id = :groupId
            order by e.createdAt desc
            """)
    List<Expense> findWithSplitsByGroupId(@Param("groupId") Long groupId);

    @Query("select coalesce(sum(e.amountPaise), 0) from Expense e where e.group.id = :groupId")
    long totalSpentByGroupId(@Param("groupId") Long groupId);

    boolean existsByPayerId(Long memberId);

    @Query("select count(s) from ExpenseSplit s where s.member.id = :memberId")
    long countSharesOfMember(@Param("memberId") Long memberId);
}
