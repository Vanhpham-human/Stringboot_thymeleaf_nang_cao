package com.example.library.service;

import com.example.library.dto.form.FinePaymentForm;
import com.example.library.dto.form.FineWaiverForm;
import com.example.library.entity.Borrowing;
import com.example.library.entity.FinePayment;
import com.example.library.entity.FineWaiver;
import com.example.library.entity.enums.BorrowingStatus;
import com.example.library.entity.enums.PaymentMethod;
import com.example.library.exception.BusinessException;
import com.example.library.exception.ConcurrentUpdateException;
import com.example.library.exception.ResourceNotFoundException;
import com.example.library.mapper.MapperUtils;
import com.example.library.repository.BorrowingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Thanh toán và miễn giảm phí phạt. Mỗi lần thanh toán / miễn giảm là một bản ghi riêng
 * (fine_payments / fine_waivers) và cập nhật số dư phí của phiếu trong cùng transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinePaymentService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BorrowingRepository borrowingRepository;
    private final Clock clock;

    /** Kết quả sau khi ghi nhận thanh toán / miễn giảm. */
    public record FineSettlementResult(BigDecimal amount, BigDecimal remainingUnpaid, BorrowingStatus status) {

        public boolean fullySettled() {
            return remainingUnpaid.signum() == 0;
        }
    }

    public FinePaymentForm newPaymentForm(Long borrowingId) {
        Borrowing borrowing = findDetailed(borrowingId);
        ensureHasDebt(borrowing);
        return FinePaymentForm.builder()
                .amount(borrowing.getUnpaidFineAmount())
                .paymentDate(today())
                .method(PaymentMethod.CASH)
                .build();
    }

    public FineWaiverForm newWaiverForm(Long borrowingId) {
        Borrowing borrowing = findDetailed(borrowingId);
        ensureHasDebt(borrowing);
        return FineWaiverForm.builder()
                .approvedDate(today())
                .build();
    }

    /** Thanh toán một phần hoặc toàn bộ; không cho thanh toán <= 0 hoặc vượt số phí còn phải thu. */
    @Transactional
    public FineSettlementResult recordPayment(Long borrowingId, FinePaymentForm form) {
        Borrowing borrowing = findDetailed(borrowingId);
        ensureHasDebt(borrowing);
        LocalDate paymentDate = form.getPaymentDate() != null ? form.getPaymentDate() : today();
        validateDate(borrowing, paymentDate, "paymentDate", "Ngày thanh toán");

        FinePayment payment = new FinePayment();
        payment.setAmount(form.getAmount());
        payment.setPaymentDate(paymentDate);
        payment.setMethod(form.getMethod() != null ? form.getMethod() : PaymentMethod.CASH);
        payment.setNote(MapperUtils.trimToNull(form.getNote()));
        borrowing.addPayment(payment);

        FineSettlementResult result = settle(borrowing, form.getAmount());
        log.info("Thanh toán {} cho phiếu #{}, còn nợ {}", form.getAmount(), borrowingId, result.remainingUnpaid());
        return result;
    }

    /** Miễn giảm phí: bắt buộc có lý do và người duyệt, không vượt số phí còn phải thu. */
    @Transactional
    public FineSettlementResult recordWaiver(Long borrowingId, FineWaiverForm form) {
        Borrowing borrowing = findDetailed(borrowingId);
        ensureHasDebt(borrowing);
        String reason = MapperUtils.trimToNull(form.getReason());
        String approvedBy = MapperUtils.trimToNull(form.getApprovedBy());
        if (reason == null) {
            throw new BusinessException("reason", "Lý do miễn giảm là bắt buộc");
        }
        if (approvedBy == null) {
            throw new BusinessException("approvedBy", "Người duyệt là bắt buộc");
        }
        LocalDate approvedDate = form.getApprovedDate() != null ? form.getApprovedDate() : today();
        validateDate(borrowing, approvedDate, "approvedDate", "Ngày duyệt");

        FineWaiver waiver = new FineWaiver();
        waiver.setAmount(form.getAmount());
        waiver.setReason(reason);
        waiver.setApprovedBy(approvedBy);
        waiver.setApprovedDate(approvedDate);
        borrowing.addWaiver(waiver);

        FineSettlementResult result = settle(borrowing, form.getAmount());
        log.info("Miễn giảm {} cho phiếu #{} (duyệt bởi {}), còn nợ {}", form.getAmount(), borrowingId, approvedBy,
                result.remainingUnpaid());
        return result;
    }

    private FineSettlementResult settle(Borrowing borrowing, BigDecimal amount) {
        borrowing.refreshStatus(today());
        try {
            borrowingRepository.saveAndFlush(borrowing);
        } catch (OptimisticLockingFailureException ex) {
            throw new ConcurrentUpdateException("Phí của phiếu vừa được cập nhật bởi giao dịch khác. Vui lòng tải lại trang và thử lại.");
        }
        return new FineSettlementResult(amount, borrowing.getUnpaidFineAmount(), borrowing.getStatus());
    }

    private void ensureHasDebt(Borrowing borrowing) {
        if (borrowing.getUnpaidFineAmount().signum() <= 0) {
            throw new BusinessException("Phiếu mượn #" + borrowing.getId() + " không còn phí phạt phải thu");
        }
    }

    private void validateDate(Borrowing borrowing, LocalDate date, String field, String label) {
        if (date.isAfter(today())) {
            throw new BusinessException(field, label + " không được ở tương lai");
        }
        if (date.isBefore(borrowing.getBorrowDate())) {
            throw new BusinessException(field,
                    label + " không được trước ngày mượn (" + borrowing.getBorrowDate().format(DATE_FORMAT) + ")");
        }
    }

    private Borrowing findDetailed(Long id) {
        return borrowingRepository.findDetailedById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("phiếu mượn", id));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
