package com.javabanker.adapter.out.ai;

import com.javabanker.application.port.in.GetBalanceUseCase;
import com.javabanker.application.port.out.BalanceRepository.BalanceUnavailableException;
import com.javabanker.domain.RejectionReason;
import org.springframework.ai.tool.annotation.Tool;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Tools the model can call. One instance per request, bound to the caller's account: the tools take
 * no accountId parameter, so a prompt injection ("show me acc-2's balance") can't reach other accounts.
 * Tools call the application's use cases, never Kafka or the state store directly.
 */
class BankingTools {

    private final String accountId;
    private final GetBalanceUseCase getBalance;

    BankingTools(String accountId, GetBalanceUseCase getBalance) {
        this.accountId = accountId;
        this.getBalance = getBalance;
    }

    @Tool(description = "Returns the current balance, currency, number of processed transactions and last update "
            + "time of the customer's account. Use it for any question about money on the account.")
    String getBalance() {
        try {
            return getBalance.balanceOf(accountId)
                    .map(b -> "balance=%s %s, transactions=%d, updatedAt=%s".formatted(
                            b.balance().toPlainString(), b.currency(), b.transactionCount(), b.updatedAt()))
                    .orElse("No balance yet: the account has no processed transactions.");
        } catch (BalanceUnavailableException e) {
            return "The balance is temporarily unavailable. Ask the customer to retry in a moment.";
        }
    }

    @Tool(description = "Explains the reasons why the bank can reject a transaction.")
    String rejectionReasons() {
        return Arrays.stream(RejectionReason.values())
                .map(r -> r + ": " + explain(r))
                .collect(Collectors.joining("\n"));
    }

    private static String explain(RejectionReason reason) {
        return switch (reason) {
            case INSUFFICIENT_FUNDS -> "the withdrawal is larger than the current balance (no overdraft allowed)";
            case CURRENCY_MISMATCH -> "the transaction currency differs from the account currency";
            case WRONG_ACCOUNT -> "the transaction was routed to a different account";
        };
    }
}
