package com.javabanker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end: HTTP -> Kafka -> Kafka Streams -> Interactive Query, against an in-JVM KRaft broker.
 * Run with {@code mvn verify}. (With Docker available, Testcontainers' KafkaContainer is the
 * closer-to-production alternative.)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.streams.state-dir=target/kafka-streams-it",
        "spring.kafka.streams.properties.commit.interval.ms=100",
        "spring.kafka.consumer.auto-offset-reset=earliest"
})
@EmbeddedKafka(kraft = true, partitions = 3, brokerProperties = {
        "transaction.state.log.replication.factor=1",
        "transaction.state.log.min.isr=1"
})
class BankerPlatformIT {

    @Autowired
    TestRestTemplate http;

    @Test
    void depositsAndWithdrawalsAreReflectedInTheBalance() {
        post("acc-it", "DEPOSIT", "100.00");
        post("acc-it", "WITHDRAWAL", "30.00");
        post("acc-it", "WITHDRAWAL", "500.00"); // rejected: insufficient funds

        await().atMost(Duration.ofSeconds(60)).pollInterval(Duration.ofMillis(500)).untilAsserted(() -> {
            ResponseEntity<Map> balance = http.getForEntity("/api/accounts/acc-it/balance", Map.class);
            assertThat(balance.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(balance.getBody()).containsEntry("balance", 70.0).containsEntry("transactionCount", 2);
        });
    }

    private void post(String account, String type, String amount) {
        var response = http.postForEntity("/api/transactions",
                Map.of("accountId", account, "type", type, "amount", amount, "currency", "EUR"), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }
}
