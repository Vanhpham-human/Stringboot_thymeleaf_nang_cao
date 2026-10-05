package com.example.library.service;

import com.example.library.entity.FinePolicy;
import com.example.library.service.FineCalculationService.FineResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FineCalculationServiceTest {

    private static final LocalDate DUE = LocalDate.of(2026, 10, 10);
    private static final FinePolicy POLICY_5000 = new FinePolicy("Test", BigDecimal.valueOf(5000), null, 0);

    private final FineCalculationService service = new FineCalculationService();

    @ParameterizedTest(name = "trả {0}, {1} cuốn -> trễ {2} ngày, phí {3}")
    @DisplayName("Bảng ví dụ: phí = số ngày trễ x số cuốn trả x 5.000")
    @CsvSource({
            "2026-10-08, 1, 0, 0",       // trả sớm
            "2026-10-10, 2, 0, 0",       // trả đúng hạn
            "2026-10-11, 1, 1, 5000",
            "2026-10-13, 1, 3, 15000",   // trễ 3 ngày
            "2026-10-13, 2, 3, 30000",
            "2026-10-20, 3, 10, 150000"
    })
    void calculatesFineFromTable(LocalDate returnDate, int quantity, long expectedLateDays, long expectedFine) {
        FineResult result = service.calculate(DUE, returnDate, quantity, POLICY_5000);

        assertThat(result.lateDays()).isEqualTo(expectedLateDays);
        assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(expectedFine));
    }

    @Test
    @DisplayName("Bảng đề bài: graceDays = 1, hạn 10/10, trả 12/10 -> chỉ tính phí 1 ngày")
    void assignmentGraceDaysExample() {
        FinePolicy policy = new FinePolicy("Ân hạn 1 ngày", BigDecimal.valueOf(5000), null, 1);

        FineResult result = service.calculate(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12), 1, policy);

        assertThat(result.lateDays()).isEqualTo(2);
        assertThat(result.chargeableDays()).isEqualTo(1);
        assertThat(result.amount()).isEqualByComparingTo("5000");
    }

    @Test
    @DisplayName("Bảng đề bài: có maxFineAmount, hạn 01/10, trả 30/10 -> phí không vượt mức trần")
    void assignmentMaxFineExample() {
        FinePolicy policy = new FinePolicy("Trần 100k", BigDecimal.valueOf(5000), BigDecimal.valueOf(100_000), 0);

        FineResult result = service.calculate(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 30), 1, policy);

        assertThat(result.lateDays()).isEqualTo(29);
        assertThat(result.amount()).isEqualByComparingTo("100000");
    }

    @Test
    @DisplayName("Ngày ân hạn được trừ khỏi số ngày trễ trước khi tính phí")
    void subtractsGraceDays() {
        FinePolicy policy = new FinePolicy("Ân hạn 2 ngày", BigDecimal.valueOf(5000), null, 2);

        assertThat(service.calculate(DUE, DUE.plusDays(2), 1, policy).amount()).isEqualByComparingTo("0");
        FineResult result = service.calculate(DUE, DUE.plusDays(5), 1, policy);
        assertThat(result.lateDays()).isEqualTo(5);
        assertThat(result.chargeableDays()).isEqualTo(3);
        assertThat(result.amount()).isEqualByComparingTo("15000");
    }

    @Test
    @DisplayName("Phí mỗi dòng sách không vượt mức trần, kể cả khi cộng dồn nhiều lần trả")
    void capsFineAtMaxAmountPerLine() {
        FinePolicy policy = new FinePolicy("Trần 100k", BigDecimal.valueOf(5000), BigDecimal.valueOf(100_000), 0);

        assertThat(service.calculate(DUE, DUE.plusDays(30), 1, policy).amount()).isEqualByComparingTo("100000");
        // Lần trước đã tính 80.000 -> lần này chỉ còn được tính tối đa 20.000
        assertThat(service.calculate(DUE, DUE.plusDays(10), 1, BigDecimal.valueOf(80_000), policy).amount())
                .isEqualByComparingTo("20000");
        assertThat(service.calculate(DUE, DUE.plusDays(10), 1, BigDecimal.valueOf(100_000), policy).amount())
                .isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Không tính phí khi số lượng trả bằng 0")
    void noFineForZeroQuantity() {
        assertThat(service.calculate(DUE, DUE.plusDays(3), 0, POLICY_5000).amount()).isEqualByComparingTo("0");
    }
}
