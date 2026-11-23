package com.javabanker.adapter.in.web;

import com.javabanker.application.port.in.GetBalanceUseCase;
import com.javabanker.application.port.in.SubmitTransactionUseCase;
import com.javabanker.application.port.in.SubmitTransactionUseCase.SubmitTransactionCommand;
import com.javabanker.domain.Transaction;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Driving adapter. Commands are asynchronous: POST returns 202 Accepted once the event is in Kafka;
 * the balance becomes visible after the stream processed it (eventual consistency / CQRS).
 */
@RestController
@RequestMapping("/api")
class TransactionController {

    private final SubmitTransactionUseCase submitTransaction;
    private final GetBalanceUseCase getBalance;

    TransactionController(SubmitTransactionUseCase submitTransaction, GetBalanceUseCase getBalance) {
        this.submitTransaction = submitTransaction;
        this.getBalance = getBalance;
    }

    @PostMapping("/transactions")
    ResponseEntity<TransactionResponse> submit(@Valid @RequestBody TransactionRequest request) {
        Transaction tx = submitTransaction.submit(new SubmitTransactionCommand(
                request.accountId(), request.type(), request.amount(), request.currency()));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(TransactionResponse.pending(tx));
    }

    /**
     * Turns an {@code Optional<AccountBalance>} into an HTTP response without any {@code if}:
     * <ul>
     *   <li>{@code map(BalanceResponse::from)}: <b>static method reference</b>, the same as
     *       {@code balance -> BalanceResponse.from(balance)}. Runs only if the Optional has a value.</li>
     *   <li>{@code map(ResponseEntity::ok)}: the same, giving {@code Optional<ResponseEntity<BalanceResponse>>}.</li>
     *   <li>{@code orElseGet(() -> ...)}: a <b>Supplier</b> called only when the Optional is empty.
     *       {@code orElse(x)} would build the 404 response every time, even when it isn't used,
     *       because its argument is evaluated before the call.</li>
     * </ul>
     * The imperative equivalent:
     * <pre>{@code
     * Optional<AccountBalance> balance = getBalance.balanceOf(accountId);
     * if (balance.isPresent()) {
     *     return ResponseEntity.ok(BalanceResponse.from(balance.get()));
     * }
     * return ResponseEntity.notFound().build();
     * }</pre>
     */
    @GetMapping("/accounts/{accountId}/balance")
    ResponseEntity<BalanceResponse> balance(@PathVariable String accountId) {
        return getBalance.balanceOf(accountId)
                .map(BalanceResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
