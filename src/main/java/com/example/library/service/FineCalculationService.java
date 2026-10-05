package com.example.library.service;

import com.example.library.entity.FinePolicy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Công thức tính phí phạt trả muộn (hàm thuần, không truy cập DB):
 * <pre>
 * lateDays       = max(0, actualReturnDate - dueDate)
 * chargeableDays = max(0, lateDays - graceDays)
 * fineAmount     = chargeableDays x returnedQuantity x dailyFineAmount
 * </pre>
 * Nếu policy có {@code maxFineAmount}, tổng phí của một dòng sách (cộng dồn qua các lần trả) không vượt mức trần.
 */
@Service
public class FineCalculationService {

    public record FineResult(long lateDays, long chargeableDays, BigDecimal amount) {

        public static FineResult none(long lateDays) {
            return new FineResult(lateDays, 0, BigDecimal.ZERO);
        }
    }

    public long lateDays(LocalDate dueDate, LocalDate actualReturnDate) {
        if (dueDate == null || actualReturnDate == null || !actualReturnDate.isAfter(dueDate)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(dueDate, actualReturnDate);
    }

    public FineResult calculate(LocalDate dueDate, LocalDate actualReturnDate, int returnedQuantity, FinePolicy policy) {
        return calculate(dueDate, actualReturnDate, returnedQuantity, BigDecimal.ZERO, policy);
    }

    /**
     * @param alreadyCharged phí đã tính cho dòng sách này ở các lần trả trước (để áp mức trần theo dòng)
     */
    public FineResult calculate(LocalDate dueDate, LocalDate actualReturnDate, int returnedQuantity,
                                BigDecimal alreadyCharged, FinePolicy policy) {
        long lateDays = lateDays(dueDate, actualReturnDate);
        long chargeableDays = Math.max(0, lateDays - Math.max(0, policy.getGraceDays()));
        if (chargeableDays == 0 || returnedQuantity <= 0) {
            return FineResult.none(lateDays);
        }

        BigDecimal amount = policy.getDailyFineAmount()
                .multiply(BigDecimal.valueOf(chargeableDays))
                .multiply(BigDecimal.valueOf(returnedQuantity));

        if (policy.getMaxFineAmount() != null) {
            BigDecimal charged = alreadyCharged == null ? BigDecimal.ZERO : alreadyCharged;
            BigDecimal remainingCap = policy.getMaxFineAmount().subtract(charged).max(BigDecimal.ZERO);
            amount = amount.min(remainingCap);
        }
        return new FineResult(lateDays, chargeableDays, amount);
    }
}
