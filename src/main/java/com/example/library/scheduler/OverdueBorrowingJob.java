package com.example.library.scheduler;

import com.example.library.service.BorrowingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Tự động đánh dấu phiếu quá hạn: chạy mỗi ngày (cron cấu hình được) và một lần khi ứng dụng khởi động. */
@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueBorrowingJob {

    private final BorrowingService borrowingService;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        run();
    }

    @Scheduled(cron = "${app.overdue-job.cron}")
    public void run() {
        int updated = borrowingService.markOverdueBorrowings();
        if (updated > 0) {
            log.info("Marked {} borrowing(s) as OVERDUE", updated);
        }
    }
}
