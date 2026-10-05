package com.example.library.mapper;

import com.example.library.dto.BorrowingDetailDto;
import com.example.library.dto.BorrowingDto;
import com.example.library.entity.Borrowing;
import com.example.library.entity.BorrowingDetail;
import com.example.library.entity.FinePayment;
import com.example.library.entity.FinePolicy;
import com.example.library.entity.FineWaiver;
import com.example.library.service.FineCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BorrowingMapper {

    private final FineCalculationService fineCalculationService;
    private final FineMapper fineMapper;

    /** Thông tin tổng quan dùng cho màn hình danh sách (không kèm chi tiết từng sách). */
    public BorrowingDto toSummaryDto(Borrowing borrowing, LocalDate today, FinePolicy policy) {
        boolean overdue = borrowing.isOverdue(today);
        BigDecimal pendingFine = overdue
                ? borrowing.getDetails().stream()
                    .map(d -> pendingFine(borrowing, d, today, policy))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;

        return BorrowingDto.builder()
                .id(borrowing.getId())
                .memberId(borrowing.getMember().getId())
                .memberName(borrowing.getMember().getFullName())
                .memberEmail(borrowing.getMember().getEmail())
                .borrowDate(borrowing.getBorrowDate())
                .dueDate(borrowing.getDueDate())
                .returnedDate(borrowing.getReturnedDate())
                .cancelledDate(borrowing.getCancelledDate())
                .cancelReason(borrowing.getCancelReason())
                .status(borrowing.getStatus())
                .note(borrowing.getNote())
                .createdAt(borrowing.getCreatedAt())
                .totalFineAmount(borrowing.getTotalFineAmount())
                .paidFineAmount(borrowing.getPaidFineAmount())
                .waivedFineAmount(borrowing.getWaivedFineAmount())
                .unpaidFineAmount(borrowing.getUnpaidFineAmount())
                .overdueDays(overdue ? fineCalculationService.lateDays(borrowing.getDueDate(), today) : 0)
                .pendingFine(pendingFine)
                .totalQuantity(borrowing.getTotalQuantity())
                .returnedQuantity(borrowing.getTotalReturnedQuantity())
                .cancellable(borrowing.isCancellable())
                .build();
    }

    /** Chi tiết phiếu: kèm từng dòng sách, lịch sử thanh toán và miễn giảm. */
    public BorrowingDto toDetailDto(Borrowing borrowing, LocalDate today, FinePolicy policy) {
        BorrowingDto dto = toSummaryDto(borrowing, today, policy);
        dto.setDetails(borrowing.getDetails().stream()
                .sorted(Comparator.comparing(BorrowingDetail::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(d -> toDetailDto(borrowing, d, today, policy))
                .toList());
        dto.setPayments(borrowing.getPayments().stream()
                .sorted(Comparator.comparing(FinePayment::getPaymentDate).reversed())
                .map(fineMapper::toDto)
                .toList());
        dto.setWaivers(borrowing.getWaivers().stream()
                .sorted(Comparator.comparing(FineWaiver::getApprovedDate).reversed())
                .map(fineMapper::toDto)
                .toList());
        return dto;
    }

    public List<BorrowingDto> toSummaryDtos(List<Borrowing> borrowings, LocalDate today, FinePolicy policy) {
        return borrowings.stream().map(b -> toSummaryDto(b, today, policy)).toList();
    }

    private BorrowingDetailDto toDetailDto(Borrowing borrowing, BorrowingDetail detail, LocalDate today, FinePolicy policy) {
        return BorrowingDetailDto.builder()
                .id(detail.getId())
                .bookId(detail.getBook().getId())
                .isbn(detail.getBook().getIsbn())
                .bookTitle(detail.getBook().getTitle())
                .author(detail.getBook().getAuthor())
                .quantity(detail.getQuantity())
                .returnedQuantity(detail.getReturnedQuantity())
                .fineAmount(detail.getFineAmount())
                .lastReturnedDate(detail.getLastReturnedDate())
                .lateDays(detail.getLateDays())
                .pendingFine(borrowing.isOpen() ? pendingFine(borrowing, detail, today, policy) : BigDecimal.ZERO)
                .build();
    }

    private BigDecimal pendingFine(Borrowing borrowing, BorrowingDetail detail, LocalDate today, FinePolicy policy) {
        if (detail.isFullyReturned()) {
            return BigDecimal.ZERO;
        }
        return fineCalculationService.calculate(borrowing.getDueDate(), today, detail.getRemainingQuantity(),
                detail.getFineAmount(), policy).amount();
    }
}
