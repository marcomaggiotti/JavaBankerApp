package com.javabanker.exercises.ex08;

import com.javabanker.exercises.ex08.Clients.AccountNotFoundException;
import com.javabanker.exercises.ex08.Clients.CardsClient;
import com.javabanker.exercises.ex08.Clients.ProfileClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("exercise")
class AccountOverviewServiceTest {

    private static final long LATENCY_MS = 300;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    private final ProfileClient profiles = accountId -> slow("Ada Lovelace");
    private final Clients.BalanceClient balances = accountId -> slow(new BigDecimal("70.00"));
    private final CardsClient cards = accountId -> slow(List.of("**** 1234"));

    @AfterEach
    void shutdown() {
        executor.shutdownNow();
    }

    @Test
    void returnsTheCombinedOverview() {
        AccountOverview overview = service(profiles, cards).overview("acc-1");

        assertThat(overview.customerName()).isEqualTo("Ada Lovelace");
        assertThat(overview.balance()).isEqualByComparingTo("70.00");
        assertThat(overview.cards()).containsExactly("**** 1234");
    }

    @Test
    void callsRunInParallel() {
        long elapsed = timeMillis(() -> service(profiles, cards).overview("acc-1"));

        assertThat(elapsed).as("about one call (%d ms), not three", LATENCY_MS).isLessThan(2 * LATENCY_MS + 100);
    }

    @Test
    void failingCardsDegradeToAnEmptyList() {
        CardsClient broken = accountId -> {
            throw new IllegalStateException("cards service down");
        };

        assertThat(service(profiles, broken).overview("acc-1").cards()).isEmpty();
    }

    @Test
    void slowCardsDegradeAfterTheTimeout() {
        CardsClient hanging = accountId -> {
            sleep(3_000);
            return List.of("never");
        };
        AccountOverview[] result = new AccountOverview[1];

        long elapsed = timeMillis(() -> result[0] = service(profiles, hanging).overview("acc-1"));

        assertThat(result[0].cards()).isEmpty();
        assertThat(result[0].customerName()).isEqualTo("Ada Lovelace");
        assertThat(elapsed).isLessThan(1_500);
    }

    @Test
    void mandatoryFailurePropagatesTheOriginalException() {
        ProfileClient unknown = accountId -> {
            throw new AccountNotFoundException(accountId);
        };

        assertThatThrownBy(() -> service(unknown, cards).overview("acc-404"))
                .isExactlyInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("acc-404");
    }

    private AccountOverviewService service(ProfileClient profileClient, CardsClient cardsClient) {
        return new AccountOverviewService(profileClient, balances, cardsClient, executor, Duration.ofMillis(500));
    }

    private static <T> T slow(T value) {
        sleep(LATENCY_MS);
        return value;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static long timeMillis(Runnable action) {
        long start = System.nanoTime();
        action.run();
        return Duration.ofNanos(System.nanoTime() - start).toMillis();
    }
}
