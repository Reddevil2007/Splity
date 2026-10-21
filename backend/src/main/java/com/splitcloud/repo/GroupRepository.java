package com.splitcloud.repo;

import com.splitcloud.domain.ExpenseGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupRepository extends JpaRepository<ExpenseGroup, Long> {

    List<ExpenseGroup> findAllByOrderByCreatedAtDesc();
}
