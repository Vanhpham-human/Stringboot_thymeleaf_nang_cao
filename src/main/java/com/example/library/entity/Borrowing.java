package com.example.library.entity;

import com.example.library.entity.enums.BorrowingStatus;
import com.example.library.exception.BusinessException;
import com.example.library.util.Money;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "borrowings")
public class Borrowing extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "borrow_date", nullable = false)
    private LocalDate borrowDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "returned_date")
    private LocalDate returnedDate;

    @Column(name = "cancelled_date")
    private LocalDate cancelledDate;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private BorrowingStatus status = BorrowingStatus.BORROWING;

    @Column(name = "total_fine_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalFineAmount = BigDecimal.ZERO;

    @Column(name = "paid_fine_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paidFineAmount = BigDecimal.ZERO;

    @Column(name = "waived_fine_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal waivedFineAmount = BigDecimal.ZERO;

    @Column(name = "unpaid_fine_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal unpaidFineAmount = BigDecimal.ZERO;

    @Column(length = 500)
    private String note;

    /** Chặn hai giao dịch cùng thanh toán / trả sách trên một phiếu ghi đè lên nhau. */
    @Version
    @Column(nullable = false)
    private Long version;

    @OneToMany(mappedBy = "borrowing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BorrowingDetail> details = new ArrayList<>();

    @OneToMany(mappedBy = "borrowing", cascade = CascadeType.ALL)
    private List<FinePayment> payments = new ArrayList<>();

    @OneToMany(mappedBy = "borrowing", cascade = CascadeType.ALL)
    private List<FineWaiver> waivers = new ArrayList<>();

    public void addDetail(BorrowingDetail detail) {
        details.add(detail);
        detail.setBorrowing(this);
    }

    public void addFine(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return;
        }
        totalFineAmount = totalFineAmount.add(amount);
        recalculateUnpaid();
    }

    public void addPayment(FinePayment payment) {
        ensureWithinUnpaid(payment.getAmount(), "amount", "thanh toán");
        paidFineAmount = paidFineAmount.add(payment.getAmount());
        recalculateUnpaid();
        payments.add(payment);
        payment.setBorrowing(this);
    }

    public void addWaiver(FineWaiver waiver) {
        ensureWithinUnpaid(waiver.getAmount(), "amount", "miễn giảm");
        waivedFineAmount = waivedFineAmount.add(waiver.getAmount());
        recalculateUnpaid();
        waivers.add(waiver);
        waiver.setBorrowing(this);
    }

    /**
     * Tính lại trạng thái từ dữ liệu thực tế của phiếu:
     * còn sách chưa trả -> OVERDUE / PARTIALLY_RETURNED / BORROWING;
     * đã trả hết -> RETURNED (không phí) / FINE_PENDING (còn nợ) / FINE_PAID (đã tất toán).
     */
    public void refreshStatus(LocalDate today) {
        if (status == BorrowingStatus.CANCELLED) {
            return;
        }
        if (!isFullyReturned()) {
            returnedDate = null;
            if (dueDate.isBefore(today)) {
                status = BorrowingStatus.OVERDUE;
            } else if (getTotalReturnedQuantity() > 0) {
                status = BorrowingStatus.PARTIALLY_RETURNED;
            } else {
                status = BorrowingStatus.BORROWING;
            }
            return;
        }
        if (totalFineAmount.signum() == 0) {
            status = BorrowingStatus.RETURNED;
        } else if (unpaidFineAmount.signum() > 0) {
            status = BorrowingStatus.FINE_PENDING;
        } else {
            status = BorrowingStatus.FINE_PAID;
        }
    }

    /** Chỉ được hủy khi phiếu chưa trả cuốn nào và chưa phát sinh phí. */
    public boolean isCancellable() {
        return status.isOpen() && getTotalReturnedQuantity() == 0 && totalFineAmount.signum() == 0;
    }

    public boolean isOpen() {
        return status.isOpen();
    }

    public boolean isFullyReturned() {
        return !details.isEmpty() && details.stream().allMatch(BorrowingDetail::isFullyReturned);
    }

    public boolean isOverdue(LocalDate today) {
        return status.isOpen() && dueDate.isBefore(today);
    }

    public int getTotalQuantity() {
        return details.stream().mapToInt(BorrowingDetail::getQuantity).sum();
    }

    public int getTotalReturnedQuantity() {
        return details.stream().mapToInt(BorrowingDetail::getReturnedQuantity).sum();
    }

    private void ensureWithinUnpaid(BigDecimal amount, String field, String action) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException(field, "Số tiền " + action + " phải lớn hơn 0");
        }
        if (amount.compareTo(unpaidFineAmount) > 0) {
            throw new BusinessException(field, "Số tiền %s (%s đ) vượt quá số phí còn phải thu (%s đ)"
                    .formatted(action, Money.format(amount), Money.format(unpaidFineAmount)));
        }
    }

    private void recalculateUnpaid() {
        unpaidFineAmount = totalFineAmount.subtract(paidFineAmount).subtract(waivedFineAmount);
    }
}
