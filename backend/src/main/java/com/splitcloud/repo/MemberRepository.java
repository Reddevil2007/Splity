package com.splitcloud.repo;

import com.splitcloud.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findByGroupIdOrderByIdAsc(Long groupId);

    long countByGroupId(Long groupId);
}
