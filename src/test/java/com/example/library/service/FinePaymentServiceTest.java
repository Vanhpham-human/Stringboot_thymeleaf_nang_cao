package com.example.library.service;

import com.example.library.dto.form.FinePaymentForm;
import com.example.library.dto.form.FineWaiverForm;
import com.example.library.entity.Book;
import com.example.library.entity.Borrowing;
import com.example.library.entity.BorrowingDetail;
import com.example.library.entity.Member;
import com.example.library.entity.enums.BorrowingStatus;
import com.example.library.entity.enums.PaymentMethod;
import com.example.library.exception.BusinessException;
import com.example.library.repository.BorrowingRepository;
import com.example.library.service.FinePaymentService.FineSettlementResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinePaymentServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);

    @Mock
    private BorrowingRepository borrowingRepository;

    private FinePaymentService service;
    private Borrowing borrowing;

    @BeforeEach
    void setUp() {
        service = new FinePaymentService(borrowingRepository,
                Clock.fixed(TODAY.atStartOfDay(ZONE).toInstant(), ZONE));
        borrowing = returnedBorrowingWithFine(BigDecimal.valueOf(30000));
        when(borrowingRepository.findDetailedById(1L)).thenReturn(Optional.of(borrowing));
        lenient().when(borrowingRepository.saveAndFlush(any(Borrowing.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Thanh toán một phần -> giảm số phí còn nợ, vẫn FINE_PENDING")
    void partialPayment() {
        FineSettlementResult result = service.recordPayment(1L, payment(10000));

        assertThat(result.remainingUnpaid()).isEqualByComparingTo("20000");
        assertThat(result.status()).isEqualTo(BorrowingStatus.FINE_PENDING);
        assertThat(borrowing.getPaidFineAmount()).isEqualByComparingTo("10000");
        assertThat(borrowing.getPayments()).hasSize(1);
    }

    @Test
    @DisplayName("Thanh toán nhiều lần đến khi hết nợ -> FINE_PAID")
    void multiplePaymentsUntilPaid() {
        service.recordPayment(1L, payment(10000));
        FineSettlementResult result = service.recordPayment(1L, payment(20000));

        assertThat(result.fullySettled()).isTrue();
        assertThat(borrowing.getStatus()).isEqualTo(BorrowingStatus.FINE_PAID);
        assertThat(borrowing.getPayments()).hasSize(2);
    }

    @Test
    @DisplayName("Thanh toán vượt số phí còn nợ bị từ chối, không thay đổi dữ liệu")
    void rejectsOverpayment() {
        assertThatThrownBy(() -> service.recordPayment(1L, payment(40000)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("vượt quá số phí còn phải thu")
                .extracting("field").isEqualTo("amount");

        assertThat(borrowing.getUnpaidFineAmount()).isEqualByComparingTo("30000");
        assertThat(borrowing.getPayments()).isEmpty();
        verify(borrowingRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Số tiền thanh toán phải lớn hơn 0")
    void rejectsZeroPayment() {
        assertThatThrownBy(() -> service.recordPayment(1L, payment(0)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("lớn hơn 0");
    }

    @Test
    @DisplayName("Ngày thanh toán không được trước ngày mượn")
    void rejectsPaymentBeforeBorrowDate() {
        FinePaymentForm form = payment(10000);
        form.setPaymentDate(borrowing.getBorrowDate().minusDays(1));

        assertThatThrownBy(() -> service.recordPayment(1L, form))
                .isInstanceOf(BusinessException.class)
                .extracting("field").isEqualTo("paymentDate");
    }

    @Test
    @DisplayName("Phiếu không còn nợ thì không cho thanh toán")
    void rejectsPaymentWhenNoDebt() {
        service.recordPayment(1L, payment(30000));

        assertThatThrownBy(() -> service.recordPayment(1L, payment(1000)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không còn phí phạt");
    }

    @Test
    @DisplayName("Miễn giảm phí: ghi nhận lý do, người duyệt và trừ vào số nợ")
    void waiverReducesUnpaid() {
        FineSettlementResult waived = service.recordWaiver(1L, waiver(10000, "Thành viên nằm viện có giấy xác nhận", "Thủ thư Mai"));
        assertThat(waived.remainingUnpaid()).isEqualByComparingTo("20000");
        assertThat(borrowing.getWaivedFineAmount()).isEqualByComparingTo("10000");
        assertThat(borrowing.getWaivers()).singleElement()
                .satisfies(w -> assertThat(w.getApprovedBy()).isEqualTo("Thủ thư Mai"));

        FineSettlementResult paid = service.recordPayment(1L, payment(20000));
        assertThat(paid.status()).isEqualTo(BorrowingStatus.FINE_PAID);
    }

    @Test
    @DisplayName("Miễn giảm bắt buộc có lý do và người duyệt")
    void waiverRequiresReasonAndApprover() {
        assertThatThrownBy(() -> service.recordWaiver(1L, waiver(10000, "  ", "Thủ thư Mai")))
                .extracting("field").isEqualTo("reason");
        assertThatThrownBy(() -> service.recordWaiver(1L, waiver(10000, "Lý do hợp lệ đủ dài", " ")))
                .extracting("field").isEqualTo("approvedBy");
        assertThat(borrowing.getWaivedFineAmount()).isEqualByComparingTo("0");
    }

    private static FinePaymentForm payment(long amount) {
        return FinePaymentForm.builder()
                .amount(BigDecimal.valueOf(amount))
                .paymentDate(TODAY)
                .method(PaymentMethod.CASH)
                .build();
    }

    private static FineWaiverForm waiver(long amount, String reason, String approvedBy) {
        return FineWaiverForm.builder()
                .amount(BigDecimal.valueOf(amount))
                .reason(reason)
                .approvedBy(approvedBy)
                .approvedDate(TODAY)
                .build();
    }

    private static Borrowing returnedBorrowingWithFine(BigDecimal fine) {
        Member member = new Member();
        member.setId(1L);
        member.setFullName("Nguyễn Văn A");
        Book book = new Book();
        book.setId(10L);
        book.setTitle("Clean Code");
        BorrowingDetail detail = new BorrowingDetail();
        detail.setId(1L);
        detail.setBook(book);
        detail.setQuantity(1);
        detail.setReturnedQuantity(1);
        detail.setFineAmount(fine);

        Borrowing borrowing = new Borrowing();
        borrowing.setId(1L);
        borrowing.setMember(member);
        borrowing.setBorrowDate(TODAY.minusDays(20));
        borrowing.setDueDate(TODAY.minusDays(6));
        borrowing.setReturnedDate(TODAY);
        borrowing.addDetail(detail);
        borrowing.addFine(fine);
        borrowing.refreshStatus(TODAY);
        return borrowing;
    }
}
