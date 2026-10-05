package com.example.library.dto;

import com.example.library.entity.enums.BorrowingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowingDto {

    private Long id;
    private Long memberId;
    private String memberName;
    private String memberEmail;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnedDate;
    private LocalDate cancelledDate;
    private String cancelReason;
    private BorrowingStatus status;
    private String note;
    private LocalDateTime createdAt;

    private BigDecimal totalFineAmount;
    private BigDecimal paidFineAmount;
    private BigDecimal waivedFineAmount;
    private BigDecimal unpaidFineAmount;

    /** Số ngày quá hạn tính đến hôm nay (0 nếu chưa quá hạn hoặc đã trả hết). */
    private long overdueDays;

    /** Phí sẽ phát sinh thêm nếu trả toàn bộ sách còn lại vào hôm nay (theo policy đang áp dụng). */
    private BigDecimal pendingFine;

    private int totalQuantity;
    private int returnedQuantity;
    private boolean cancellable;

    @Builder.Default
    private List<BorrowingDetailDto> details = new ArrayList<>();

    @Builder.Default
    private List<FinePaymentDto> payments = new ArrayList<>();

    @Builder.Default
    private List<FineWaiverDto> waivers = new ArrayList<>();

    public int getRemainingQuantity() {
        return totalQuantity - returnedQuantity;
    }

    /** Phí đã phát sinh + phí dự kiến nếu trả nốt hôm nay. */
    public BigDecimal getEstimatedFine() {
        return totalFineAmount.add(pendingFine == null ? BigDecimal.ZERO : pendingFine);
    }

    public boolean isOpen() {
        return status != null && status.isOpen();
    }

    public boolean hasUnpaidFine() {
        return unpaidFineAmount != null && unpaidFineAmount.signum() > 0;
    }

    public boolean isCancelled() {
        return status == BorrowingStatus.CANCELLED;
    }
}
