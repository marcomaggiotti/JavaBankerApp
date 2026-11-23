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
                    // Function<AccountBalance, String> as a lambda: converts the balance into text for the LLM.
                    // A one-expression lambda needs no braces and no "return".
                    .map(b -> "balance=%s %s, transactions=%d, updatedAt=%s".formatted(
                            b.balance().toPlainString(), b.currency(), b.transactionCount(), b.updatedAt()))
                    // orElse (not orElseGet) is fine here: the fallback is a constant String, so it costs nothing.
                    .orElse("No balance yet: the account has no processed transactions.");
        } catch (BalanceUnavailableException e) {
            return "The balance is temporarily unavailable. Ask the customer to retry in a moment.";
        }
    }

    @Tool(description = "Explains the reasons why the bank can reject a transaction.")
    String rejectionReasons() {
        return Arrays.stream(RejectionReason.values())
                // Stream.map with a lambda: each enum constant r becomes "NAME: explanation".
                .map(r -> r + ": " + explain(r))
                // Collectors.joining concatenates all the Strings with a separator.
                .collect(Collectors.joining("\n"));
    }

    /**
     * NOT a lambda: {@code case X -> value} is the arrow form of a <b>switch expression</b> (Java 14+).
     * It shares the {@code ->} symbol with lambdas but means "this case yields this value":
     * no fall-through, no {@code break}. Over an enum the compiler checks that every constant is
     * covered, so adding a new RejectionReason without an explanation fails the build.
     */
    private static String explain(RejectionReason reason) {
        return switch (reason) {
            case INSUFFICIENT_FUNDS -> "the withdrawal is larger than the current balance (no overdraft allowed)";
            case CURRENCY_MISMATCH -> "the transaction currency differs from the account currency";
            case WRONG_ACCOUNT -> "the transaction was routed to a different account";
        };
    }
}
