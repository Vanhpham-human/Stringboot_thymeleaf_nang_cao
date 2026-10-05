package com.example.library.integration;

import com.example.library.dto.BorrowingDto;
import com.example.library.dto.form.FinePaymentForm;
import com.example.library.dto.form.ReturnForm;
import com.example.library.dto.form.ReturnItemForm;
import com.example.library.entity.Book;
import com.example.library.entity.Member;
import com.example.library.entity.enums.BorrowingStatus;
import com.example.library.entity.enums.PaymentMethod;
import com.example.library.exception.BusinessException;
import com.example.library.repository.BorrowingRepository;
import com.example.library.service.BorrowingService;
import com.example.library.service.FineCalculationService;
import com.example.library.service.FinePaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.reset;

class BorrowingFlowIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private BorrowingService borrowingService;
    @Autowired
    private FinePaymentService finePaymentService;
    @Autowired
    private BorrowingRepository borrowingRepository;
    @SpyBean
    private FineCalculationService fineCalculationService;
    @Autowired
    private Clock clock;

    private LocalDate today;

    @BeforeEach
    void initToday() {
        today = LocalDate.now(clock);
    }

    @Test
    @DisplayName("Mượn nhiều sách mà một sách không đủ tồn kho -> rollback toàn bộ, không trừ kho sách nào")
    void insufficientStockRollsBackEverything() {
        Member member = member("Nguyễn Văn A");
        Book enough = book("Clean Code", 5, 5);
        Book notEnough = book("Refactoring", 2, 1);

        assertThatThrownBy(() -> borrowingService.createBorrowing(
                borrowingForm(member, today, today.plusDays(14), item(enough, 2), item(notEnough, 2))))
                .isInstanceOf(BusinessException.class);

        assertThat(available(enough)).isEqualTo(5);
        assertThat(available(notEnough)).isEqualTo(1);
        assertThat(borrowingRepository.count()).isZero();
    }

    @Test
    @DisplayName("Lỗi khi đang tính phí ở dòng thứ hai -> rollback cả tồn kho, số lượng đã trả và phí của dòng đầu")
    void returnRollsBackWhenFineCalculationFails() {
        Member member = member("Trần Thị B");
        Book first = book("Domain-Driven Design", 5, 5);
        Book second = book("Effective Java", 5, 5);
        Long id = borrowingService.createBorrowing(
                borrowingForm(member, today.minusDays(17), today.minusDays(3), item(first, 1), item(second, 1)));
        doCallRealMethod()
                .doThrow(new IllegalStateException("Lỗi giả lập khi tính phí"))
                .when(fineCalculationService).calculate(any(), any(), anyInt(), any(BigDecimal.class), any());

        assertThatThrownBy(() -> borrowingService.returnAll(id, today))
                .isInstanceOf(IllegalStateException.class);
        reset(fineCalculationService);

        BorrowingDto after = borrowingService.getDetail(id);
        assertThat(after.getReturnedQuantity()).isZero();
        assertThat(after.getTotalFineAmount()).isEqualByComparingTo("0");
        assertThat(after.getStatus()).isEqualTo(BorrowingStatus.OVERDUE);
        assertThat(available(first)).isEqualTo(4);
        assertThat(available(second)).isEqualTo(4);
    }

    @Test
    @DisplayName("Trả muộn 3 ngày -> nợ 15.000; còn nợ thì bị chặn mượn; trả góp đến hết thì được mượn lại")
    void lateReturnPaymentAndDebtBlocking() {
        Member member = member("Lê Văn C");
        Book book = book("The Pragmatic Programmer", 3, 3);
        Long id = borrowingService.createBorrowing(
                borrowingForm(member, today.minusDays(17), today.minusDays(3), item(book, 1)));

        BorrowingDto returned = borrowingService.returnAll(id, today);
        assertThat(returned.getTotalFineAmount()).isEqualByComparingTo("15000");
        assertThat(returned.getStatus()).isEqualTo(BorrowingStatus.FINE_PENDING);
        assertThat(available(book)).isEqualTo(3);

        assertThatThrownBy(() -> borrowingService.createBorrowing(
                borrowingForm(member, today, today.plusDays(14), item(book, 1))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("còn nợ 15.000 đ");

        finePaymentService.recordPayment(id, payment(10000));
        assertThat(borrowingService.getDetail(id).getUnpaidFineAmount()).isEqualByComparingTo("5000");

        assertThatThrownBy(() -> finePaymentService.recordPayment(id, payment(6000)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("vượt quá");

        finePaymentService.recordPayment(id, payment(5000));
        BorrowingDto paid = borrowingService.getDetail(id);
        assertThat(paid.getStatus()).isEqualTo(BorrowingStatus.FINE_PAID);
        assertThat(paid.getPayments()).hasSize(2);

        Long next = borrowingService.createBorrowing(borrowingForm(member, today, today.plusDays(14), item(book, 1)));
        assertThat(next).isNotNull();
        assertThat(available(book)).isEqualTo(2);
    }

    @Test
    @DisplayName("Trả một phần khi trễ hạn: chỉ tính phí số cuốn trả lần đó, tồn kho cộng đúng số trả")
    void partialLateReturn() {
        Member member = member("Phạm Thị D");
        Book book = book("Clean Architecture", 5, 5);
        Long id = borrowingService.createBorrowing(
                borrowingForm(member, today.minusDays(17), today.minusDays(3), item(book, 3)));
        Long detailId = borrowingService.getDetail(id).getDetails().get(0).getId();

        ReturnForm form = new ReturnForm();
        form.setReturnDate(today);
        form.setItems(List.of(new ReturnItemForm(detailId, 1)));
        BorrowingDto result = borrowingService.returnBooks(id, form);

        assertThat(result.getTotalFineAmount()).isEqualByComparingTo("15000");
        assertThat(result.getReturnedQuantity()).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo(BorrowingStatus.OVERDUE);
        assertThat(available(book)).isEqualTo(3);
    }

    private FinePaymentForm payment(long amount) {
        return FinePaymentForm.builder()
                .amount(BigDecimal.valueOf(amount))
                .paymentDate(today)
                .method(PaymentMethod.CASH)
                .build();
    }
}
