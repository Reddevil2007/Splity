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

    @PutMapping("/{groupId}/members/{memberId}")
    public MemberView updateMember(@PathVariable long groupId, @PathVariable long memberId,
                                   @Valid @RequestBody AddMemberRequest request) {
        return service.updateMember(groupId, memberId, request);
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMember(@PathVariable long groupId, @PathVariable long memberId) {
        service.deleteMember(groupId, memberId);
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

    @PutMapping("/{groupId}/expenses/{expenseId}")
    public ExpenseView updateExpense(@PathVariable long groupId, @PathVariable long expenseId,
                                     @Valid @RequestBody UpdateExpenseRequest request) {
        return service.updateExpense(groupId, expenseId, request);
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

    @PutMapping("/{groupId}/settlements/{settlementId}")
    public SettlementView updateSettlement(@PathVariable long groupId, @PathVariable long settlementId,
                                           @Valid @RequestBody UpdateSettlementRequest request) {
        return service.updateSettlement(groupId, settlementId, request);
    }

    /** Undo a payment that was marked as paid by mistake. */
    @DeleteMapping("/{groupId}/settlements/{settlementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSettlement(@PathVariable long groupId, @PathVariable long settlementId) {
        service.deleteSettlement(groupId, settlementId);
    }
}
