package com.splitcloud.web;

import com.splitcloud.service.GroupService;
import com.splitcloud.web.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * REST endpoints for groups and everything inside a group.
 * Each resource has a URI and is acted on with the standard HTTP verbs.
 */
@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService service;

    public GroupController(GroupService service) {
        this.service = service;
    }

    @GetMapping
    public List<GroupSummary> listGroups() {
        return service.listGroups();
    }

    @PostMapping
    public ResponseEntity<GroupSummary> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        GroupSummary created = service.createGroup(request);
        return ResponseEntity.created(URI.create("/api/groups/" + created.id())).body(created);
    }

    @GetMapping("/{groupId}")
    public GroupDetail getGroup(@PathVariable long groupId) {
        return service.getGroup(groupId);
    }

    @PostMapping("/{groupId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberView addMember(@PathVariable long groupId, @Valid @RequestBody AddMemberRequest request) {
        return service.addMember(groupId, request);
    }

    @GetMapping("/{groupId}/expenses")
    public List<ExpenseView> listExpenses(@PathVariable long groupId) {
        return service.listExpenses(groupId);
    }

    @PostMapping("/{groupId}/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseView addExpense(@PathVariable long groupId, @Valid @RequestBody CreateExpenseRequest request) {
        return service.addExpense(groupId, request);
    }

    @DeleteMapping("/{groupId}/expenses/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(@PathVariable long groupId, @PathVariable long expenseId) {
        service.deleteExpense(groupId, expenseId);
    }

    @GetMapping("/{groupId}/balances")
    public List<BalanceView> balances(@PathVariable long groupId) {
        return service.balances(groupId);
    }

    @GetMapping("/{groupId}/settlement-plan")
    public List<TransferView> settlementPlan(@PathVariable long groupId) {
        return service.settlementPlan(groupId);
    }

    @GetMapping("/{groupId}/settlements")
    public List<SettlementView> listSettlements(@PathVariable long groupId) {
        return service.listSettlements(groupId);
    }

    @PostMapping("/{groupId}/settlements")
    @ResponseStatus(HttpStatus.CREATED)
    public SettlementView recordSettlement(@PathVariable long groupId,
                                           @Valid @RequestBody RecordSettlementRequest request) {
        return service.recordSettlement(groupId, request);
    }
}
