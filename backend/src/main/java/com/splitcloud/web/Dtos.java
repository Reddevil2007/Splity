package com.splitcloud.web;

import com.splitcloud.domain.ExpenseCategory;
import com.splitcloud.domain.SplitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Request and response shapes of the REST API (Representational State Transfer).
 * Requests carry Bean Validation annotations; @Valid on the controller rejects
 * bad input with 400 Bad Request before any business code runs.
 * All money values are integers in paise.
 */
public final class Dtos {

    private Dtos() {
    }

    // ---------- requests ----------

    public record CreateGroupRequest(
            @NotBlank(message = "Group name is required") @Size(max = 80) String name) {
    }

    public record AddMemberRequest(
            @NotBlank(message = "Member name is required") @Size(max = 60) String name,
            @Email(message = "Email is not valid") @Size(max = 120) String email,
            @Size(max = 80)
            @Pattern(regexp = "^$|^[\\w.\\-]{2,64}@[A-Za-z]{2,64}$", message = "UPI ID should look like name@bank")
            String upiId) {
    }

    public record ParticipantRequest(@NotNull Long memberId, BigDecimal value) {
    }

    public record CreateExpenseRequest(
            @NotBlank(message = "Description is required") @Size(max = 120) String description,
            @Positive(message = "Amount must be greater than zero") long amountPaise,
            @NotNull(message = "Choose who paid") Long payerId,
            @NotNull SplitType splitType,
            @NotEmpty(message = "Pick at least one participant") @Valid List<ParticipantRequest> participants,
            ExpenseCategory category,
            LocalDate spentOn) {
    }

    /** Edits the parts of an expense that don't affect the split. Null fields are left unchanged. */
    public record UpdateExpenseRequest(
            @Size(min = 1, max = 120, message = "Description must be 1 to 120 characters") String description,
            ExpenseCategory category,
            LocalDate spentOn) {
    }

    public record RecordSettlementRequest(
            @NotNull Long fromMemberId,
            @NotNull Long toMemberId,
            @Positive(message = "Amount must be greater than zero") long amountPaise,
            LocalDate paidOn) {
    }

    public record UpdateSettlementRequest(@NotNull(message = "Choose a date") LocalDate paidOn) {
    }

    // ---------- responses ----------

    public record GroupSummary(long id, String name, long memberCount, long totalSpentPaise, Instant createdAt) {
    }

    public record MemberView(long id, String name, String email, String upiId) {
    }

    public record GroupDetail(long id, String name, Instant createdAt, long totalSpentPaise,
                              List<MemberView> members) {
    }

    public record SplitView(long memberId, String memberName, long sharePaise) {
    }

    public record ExpenseView(long id, String description, long amountPaise, SplitType splitType,
                              ExpenseCategory category, LocalDate spentOn,
                              long payerId, String payerName, Instant createdAt, List<SplitView> splits) {
    }

    /** net = paid - share + settledOut - settledIn; positive means the member is owed money. */
    public record BalanceView(long memberId, String memberName, long paidPaise, long sharePaise,
                              long netPaise) {
    }

    public record TransferView(long fromMemberId, String fromName, long toMemberId, String toName,
                               long amountPaise, String upiLink) {
    }

    public record SettlementView(long id, long fromMemberId, String fromName, long toMemberId, String toName,
                                 long amountPaise, LocalDate paidOn, Instant createdAt) {
    }

    public record ReminderView(long groupId, String groupName, long memberId, String memberName, String email,
                               long owesPaise) {
    }

    public record ErrorResponse(int status, String error, String message) {
    }
}
