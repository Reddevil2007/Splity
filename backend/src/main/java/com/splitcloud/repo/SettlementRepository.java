package com.splitcloud.repo;

import com.splitcloud.domain.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    @Query("""
            select s from Settlement s
            join fetch s.fromMember
            join fetch s.toMember
            where s.group.id = :groupId
            order by s.createdAt desc
            """)
    List<Settlement> findWithMembersByGroupId(@Param("groupId") Long groupId);

    boolean existsByFromMemberIdOrToMemberId(Long fromMemberId, Long toMemberId);
}
