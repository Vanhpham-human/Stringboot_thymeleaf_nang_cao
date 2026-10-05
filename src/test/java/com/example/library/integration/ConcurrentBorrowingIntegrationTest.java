package com.example.library.integration;

import com.example.library.entity.Book;
import com.example.library.entity.Member;
import com.example.library.repository.BorrowingRepository;
import com.example.library.service.BorrowingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ConcurrentBorrowingIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private BorrowingService borrowingService;
    @Autowired
    private BorrowingRepository borrowingRepository;
    @Autowired
    private Clock clock;

    @Test
    @DisplayName("Hai người cùng mượn cuốn cuối cùng: đúng một người thành công, tồn kho không âm")
    void onlyOneConcurrentBorrowingGetsTheLastCopy() throws Exception {
        Book lastCopy = book("Cuốn cuối cùng", 1, 1);
        Member first = member("Người mượn 1");
        Member second = member("Người mượn 2");
        LocalDate today = LocalDate.now(clock);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Outcome>> futures = new ArrayList<>();
            for (Member member : List.of(first, second)) {
                Callable<Outcome> task = () -> {
                    ready.countDown();
                    start.await();
                    try {
                        borrowingService.createBorrowing(borrowingForm(member, today, today.plusDays(14), item(lastCopy, 1)));
                        return Outcome.success();
                    } catch (RuntimeException ex) {
                        return Outcome.failure(ex);
                    }
                };
                futures.add(executor.submit(task));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Outcome> outcomes = new ArrayList<>();
            for (Future<Outcome> future : futures) {
                outcomes.add(future.get(30, TimeUnit.SECONDS));
            }

            assertThat(outcomes.stream().filter(Outcome::succeeded).count())
                    .as("Kết quả hai giao dịch: %s", outcomes)
                    .isEqualTo(1);
            assertThat(available(lastCopy)).isZero();
            assertThat(borrowingRepository.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private record Outcome(boolean succeeded, String error) {

        static Outcome success() {
            return new Outcome(true, null);
        }

        static Outcome failure(RuntimeException ex) {
            return new Outcome(false, ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }
}
