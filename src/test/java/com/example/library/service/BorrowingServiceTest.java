package com.example.library.service;

import com.example.library.config.BorrowingProperties;
import com.example.library.dto.BorrowingDto;
import com.example.library.dto.form.BorrowingForm;
import com.example.library.dto.form.BorrowingItemForm;
import com.example.library.dto.form.ReturnForm;
import com.example.library.dto.form.ReturnItemForm;
import com.example.library.entity.Book;
import com.example.library.entity.Borrowing;
import com.example.library.entity.BorrowingDetail;
import com.example.library.entity.FinePolicy;
import com.example.library.entity.Member;
import com.example.library.entity.enums.BookStatus;
import com.example.library.entity.enums.BorrowingStatus;
import com.example.library.entity.enums.MemberStatus;
import com.example.library.exception.BusinessException;
import com.example.library.exception.ConcurrentUpdateException;
import com.example.library.mapper.BorrowingMapper;
import com.example.library.mapper.FineMapper;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BorrowingRepository;
import com.example.library.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BorrowingServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);
    private static final FinePolicy POLICY = new FinePolicy("Chính sách test", BigDecimal.valueOf(5000), null, 0);

    @Mock
    private BorrowingRepository borrowingRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private FinePolicyService finePolicyService;

    private BorrowingService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(TODAY.atStartOfDay(ZONE).toInstant(), ZONE);
        FineCalculationService fineCalculationService = new FineCalculationService();
        BorrowingProperties properties = new BorrowingProperties(14, 5, BigDecimal.valueOf(5000));
        service = new BorrowingService(borrowingRepository, memberRepository, bookRepository,
                new BorrowingMapper(fineCalculationService, new FineMapper()), fineCalculationService,
                finePolicyService, properties, clock);
        lenient().when(finePolicyService.getActivePolicy()).thenReturn(POLICY);
        lenient().when(borrowingRepository.sumUnpaidFineByMember(anyLong())).thenReturn(BigDecimal.ZERO);
    }

    @Nested
    @DisplayName("Tạo phiếu mượn")
    class CreateBorrowing {

        @Test
        @DisplayName("Mượn nhiều sách thành công: trừ tồn kho từng sách, hạn trả mặc định 14 ngày")
        void borrowsMultipleBooks() {
            Book cleanCode = book(10L, "Clean Code", 5, 3);
            Book refactoring = book(11L, "Refactoring", 2, 2);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member(1L, MemberStatus.ACTIVE)));
            when(bookRepository.findById(10L)).thenReturn(Optional.of(cleanCode));
            when(bookRepository.findById(11L)).thenReturn(Optional.of(refactoring));
            stubSaveAndFlush();

            Long id = service.createBorrowing(form(1L, item(10L, 2), item(11L, 1)));

            assertThat(id).isEqualTo(100L);
            assertThat(cleanCode.getAvailableQuantity()).isEqualTo(1);
            assertThat(refactoring.getAvailableQuantity()).isEqualTo(1);
            Borrowing saved = captureSaved();
            assertThat(saved.getDueDate()).isEqualTo(TODAY.plusDays(14));
            assertThat(saved.getStatus()).isEqualTo(BorrowingStatus.BORROWING);
            assertThat(saved.getDetails()).hasSize(2);
            assertThat(saved.getTotalFineAmount()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("Một sách không đủ tồn kho -> báo lỗi, không lưu phiếu")
        void rejectsInsufficientStock() {
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member(1L, MemberStatus.ACTIVE)));
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book(10L, "Clean Code", 5, 5)));
            when(bookRepository.findById(11L)).thenReturn(Optional.of(book(11L, "Refactoring", 2, 1)));

            assertThatThrownBy(() -> service.createBorrowing(form(1L, item(10L, 1), item(11L, 2))))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("chỉ còn 1 cuốn");
            verify(borrowingRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Không cho chọn trùng một sách trên nhiều dòng")
        void rejectsDuplicateBooks() {
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member(1L, MemberStatus.ACTIVE)));
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book(10L, "Clean Code", 5, 5)));

            assertThatThrownBy(() -> service.createBorrowing(form(1L, item(10L, 1), item(10L, 2))))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("nhiều dòng")
                    .extracting("field").isEqualTo("items");
        }

        @Test
        @DisplayName("Phiếu phải có ít nhất một sách và số lượng > 0")
        void rejectsEmptyOrZeroQuantity() {
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member(1L, MemberStatus.ACTIVE)));

            assertThatThrownBy(() -> service.createBorrowing(form(1L)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ít nhất một");
            assertThatThrownBy(() -> service.createBorrowing(form(1L, item(10L, 0))))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("lớn hơn 0");
        }

        @Test
        @DisplayName("Không cho thành viên bị khóa mượn sách")
        void rejectsBlockedMember() {
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member(1L, MemberStatus.BLOCKED)));

            assertThatThrownBy(() -> service.createBorrowing(form(1L, item(10L, 1))))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("đang bị khóa")
                    .extracting("field").isEqualTo("memberId");
        }

        @Test
        @DisplayName("Thành viên còn nợ phí phạt bị chặn mượn mới")
        void rejectsMemberWithUnpaidFine() {
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member(1L, MemberStatus.ACTIVE)));
            when(borrowingRepository.sumUnpaidFineByMember(1L)).thenReturn(BigDecimal.valueOf(15000));

            assertThatThrownBy(() -> service.createBorrowing(form(1L, item(10L, 1))))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("còn nợ 15.000 đ")
                    .extracting("field").isEqualTo("memberId");
            verify(bookRepository, never()).findById(any());
            verify(borrowingRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Thành viên có phiếu quá hạn bị chặn mượn mới")
        void rejectsMemberWithOverdueBorrowing() {
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member(1L, MemberStatus.ACTIVE)));
            when(borrowingRepository.existsOverdueByMember(1L, TODAY)).thenReturn(true);

            assertThatThrownBy(() -> service.createBorrowing(form(1L, item(10L, 1))))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("quá hạn");
        }

        @Test
        @DisplayName("Không cho giữ quá 5 cuốn chưa trả")
        void rejectsMoreThanMaxBooks() {
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member(1L, MemberStatus.ACTIVE)));
            when(borrowingRepository.countUnreturnedBooksByMember(1L)).thenReturn(4L);

            assertThatThrownBy(() -> service.createBorrowing(form(1L, item(10L, 2))))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("giới hạn 5 cuốn");
            verify(bookRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Xung đột @Version khi lưu được chuyển thành ConcurrentUpdateException")
        void translatesOptimisticLockFailure() {
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member(1L, MemberStatus.ACTIVE)));
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book(10L, "Clean Code", 1, 1)));
            when(borrowingRepository.saveAndFlush(any(Borrowing.class)))
                    .thenThrow(new ObjectOptimisticLockingFailureException(Book.class, 10L));

            assertThatThrownBy(() -> service.createBorrowing(form(1L, item(10L, 1))))
                    .isInstanceOf(ConcurrentUpdateException.class);
        }
    }

    @Nested
    @DisplayName("Trả sách và tính phí")
    class ReturnBooks {

        @Test
        @DisplayName("Trả đủ đúng hạn -> RETURNED, không phát sinh phí, hoàn tồn kho")
        void returnAllOnTime() {
            Book book = book(10L, "Clean Code", 5, 3);
            Borrowing borrowing = borrowing(TODAY.minusDays(5), TODAY.plusDays(9), detail(1L, book, 2));
            when(borrowingRepository.findDetailedById(1L)).thenReturn(Optional.of(borrowing));
            stubSaveAndFlush();

            BorrowingDto result = service.returnAll(1L, TODAY);

            assertThat(result.getStatus()).isEqualTo(BorrowingStatus.RETURNED);
            assertThat(result.getTotalFineAmount()).isEqualByComparingTo("0");
            assertThat(result.getReturnedDate()).isEqualTo(TODAY);
            assertThat(book.getAvailableQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("Trả một phần đúng hạn -> PARTIALLY_RETURNED")
        void partialReturnOnTime() {
            Book book = book(10L, "Clean Code", 5, 2);
            Borrowing borrowing = borrowing(TODAY.minusDays(5), TODAY.plusDays(9), detail(1L, book, 3));
            when(borrowingRepository.findDetailedById(1L)).thenReturn(Optional.of(borrowing));
            stubSaveAndFlush();

            BorrowingDto result = service.returnBooks(1L, returnForm(TODAY, new ReturnItemForm(1L, 1)));

            assertThat(result.getStatus()).isEqualTo(BorrowingStatus.PARTIALLY_RETURNED);
            assertThat(result.getReturnedQuantity()).isEqualTo(1);
            assertThat(result.getReturnedDate()).isNull();
            assertThat(result.getTotalFineAmount()).isEqualByComparingTo("0");
            assertThat(book.getAvailableQuantity()).isEqualTo(3);
        }

        @Test
        @DisplayName("Trả muộn 3 ngày 1 cuốn -> phí 15.000, trạng thái FINE_PENDING")
        void lateThreeDaysFine() {
            Book book = book(10L, "Clean Code", 5, 4);
            Borrowing borrowing = borrowing(TODAY.minusDays(17), TODAY.minusDays(3), detail(1L, book, 1));
            when(borrowingRepository.findDetailedById(1L)).thenReturn(Optional.of(borrowing));
            stubSaveAndFlush();

            BorrowingDto result = service.returnAll(1L, TODAY);

            assertThat(result.getTotalFineAmount()).isEqualByComparingTo("15000");
            assertThat(result.getUnpaidFineAmount()).isEqualByComparingTo("15000");
            assertThat(result.getStatus()).isEqualTo(BorrowingStatus.FINE_PENDING);
            assertThat(result.getDetails().get(0).getLateDays()).isEqualTo(3);
        }

        @Test
        @DisplayName("Trả một phần khi đã trễ -> chỉ tính phí số cuốn trả lần đó")
        void partialLateReturnChargesOnlyReturnedQuantity() {
            Book book = book(10L, "Clean Code", 5, 2);
            BorrowingDetail detail = detail(1L, book, 3);
            Borrowing borrowing = borrowing(TODAY.minusDays(17), TODAY.minusDays(3), detail);
            when(borrowingRepository.findDetailedById(1L)).thenReturn(Optional.of(borrowing));
            stubSaveAndFlush();

            BorrowingDto first = service.returnBooks(1L, returnForm(TODAY, new ReturnItemForm(1L, 1)));

            assertThat(first.getTotalFineAmount()).isEqualByComparingTo("15000");
            assertThat(first.getStatus()).isEqualTo(BorrowingStatus.OVERDUE);
            assertThat(detail.getFineAmount()).isEqualByComparingTo("15000");

            BorrowingDto second = service.returnBooks(1L, returnForm(TODAY, new ReturnItemForm(1L, 2)));

            assertThat(second.getTotalFineAmount()).isEqualByComparingTo("45000");
            assertThat(second.getStatus()).isEqualTo(BorrowingStatus.FINE_PENDING);
            assertThat(book.getAvailableQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("Không cho trả nhiều hơn số lượng còn đang mượn")
        void rejectsReturningMoreThanRemaining() {
            Book book = book(10L, "Clean Code", 5, 3);
            Borrowing borrowing = borrowing(TODAY.minusDays(5), TODAY.plusDays(9), detail(1L, book, 2));
            when(borrowingRepository.findDetailedById(1L)).thenReturn(Optional.of(borrowing));

            assertThatThrownBy(() -> service.returnBooks(1L, returnForm(TODAY, new ReturnItemForm(1L, 3))))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("chỉ còn 2 cuốn");
            assertThat(book.getAvailableQuantity()).isEqualTo(3);
            verify(borrowingRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Không cho trả sách trên phiếu đã trả đủ hoặc đã hủy")
        void rejectsClosedBorrowings() {
            Borrowing returned = borrowing(TODAY.minusDays(5), TODAY.plusDays(9), detail(1L, book(10L, "A", 1, 1), 1));
            returned.setStatus(BorrowingStatus.RETURNED);
            Borrowing cancelled = borrowing(TODAY.minusDays(5), TODAY.plusDays(9), detail(2L, book(11L, "B", 1, 1), 1));
            cancelled.setStatus(BorrowingStatus.CANCELLED);
            when(borrowingRepository.findDetailedById(1L)).thenReturn(Optional.of(returned));
            when(borrowingRepository.findDetailedById(2L)).thenReturn(Optional.of(cancelled));

            assertThatThrownBy(() -> service.returnAll(1L, TODAY)).hasMessageContaining("đã được trả đủ");
            assertThatThrownBy(() -> service.returnAll(2L, TODAY)).hasMessageContaining("đã bị hủy");
        }

        @Test
        @DisplayName("Ngày trả không được ở tương lai")
        void rejectsFutureReturnDate() {
            Borrowing borrowing = borrowing(TODAY.minusDays(5), TODAY.plusDays(9), detail(1L, book(10L, "A", 2, 1), 1));
            when(borrowingRepository.findDetailedById(1L)).thenReturn(Optional.of(borrowing));

            assertThatThrownBy(() -> service.returnAll(1L, TODAY.plusDays(1)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("field").isEqualTo("returnDate");
        }
    }

    @Nested
    @DisplayName("Hủy phiếu")
    class Cancel {

        @Test
        @DisplayName("Hủy phiếu chưa trả cuốn nào -> CANCELLED và hoàn tồn kho")
        void cancelsAndRestoresStock() {
            Book book = book(10L, "Clean Code", 5, 3);
            Borrowing borrowing = borrowing(TODAY, TODAY.plusDays(14), detail(1L, book, 2));
            when(borrowingRepository.findDetailedById(1L)).thenReturn(Optional.of(borrowing));
            stubSaveAndFlush();

            service.cancel(1L, "Lập nhầm thành viên");

            assertThat(borrowing.getStatus()).isEqualTo(BorrowingStatus.CANCELLED);
            assertThat(borrowing.getCancelReason()).isEqualTo("Lập nhầm thành viên");
            assertThat(book.getAvailableQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("Không cho hủy khi đã trả một phần")
        void rejectsCancelAfterPartialReturn() {
            BorrowingDetail detail = detail(1L, book(10L, "Clean Code", 5, 4), 2);
            detail.setReturnedQuantity(1);
            Borrowing borrowing = borrowing(TODAY.minusDays(3), TODAY.plusDays(11), detail);
            borrowing.setStatus(BorrowingStatus.PARTIALLY_RETURNED);
            when(borrowingRepository.findDetailedById(1L)).thenReturn(Optional.of(borrowing));

            assertThatThrownBy(() -> service.cancel(1L, "Đổi ý"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("chưa trả cuốn nào");
        }
    }

    private void stubSaveAndFlush() {
        when(borrowingRepository.saveAndFlush(any(Borrowing.class))).thenAnswer(inv -> {
            Borrowing b = inv.getArgument(0);
            if (b.getId() == null) {
                b.setId(100L);
            }
            return b;
        });
    }

    private Borrowing captureSaved() {
        ArgumentCaptor<Borrowing> captor = ArgumentCaptor.forClass(Borrowing.class);
        verify(borrowingRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private static Member member(Long id, MemberStatus status) {
        Member member = new Member();
        member.setId(id);
        member.setFullName("Nguyễn Văn A");
        member.setEmail("a@example.com");
        member.setStatus(status);
        return member;
    }

    private static Book book(Long id, String title, int total, int available) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setIsbn("978000000000" + id % 10);
        book.setAuthor("Tác giả");
        book.setTotalQuantity(total);
        book.setAvailableQuantity(available);
        book.setStatus(BookStatus.ACTIVE);
        return book;
    }

    private static BorrowingDetail detail(Long id, Book book, int quantity) {
        BorrowingDetail detail = new BorrowingDetail();
        detail.setId(id);
        detail.setBook(book);
        detail.setQuantity(quantity);
        detail.setReturnedQuantity(0);
        detail.setFineAmount(BigDecimal.ZERO);
        return detail;
    }

    private static Borrowing borrowing(LocalDate borrowDate, LocalDate dueDate, BorrowingDetail... details) {
        Borrowing borrowing = new Borrowing();
        borrowing.setId(1L);
        borrowing.setMember(member(1L, MemberStatus.ACTIVE));
        borrowing.setBorrowDate(borrowDate);
        borrowing.setDueDate(dueDate);
        for (BorrowingDetail detail : details) {
            borrowing.addDetail(detail);
        }
        borrowing.refreshStatus(TODAY);
        return borrowing;
    }

    private static BorrowingItemForm item(Long bookId, int quantity) {
        return new BorrowingItemForm(bookId, quantity);
    }

    private static BorrowingForm form(Long memberId, BorrowingItemForm... items) {
        BorrowingForm form = new BorrowingForm();
        form.setMemberId(memberId);
        form.setBorrowDate(TODAY);
        form.setItems(List.of(items));
        return form;
    }

    private static ReturnForm returnForm(LocalDate returnDate, ReturnItemForm... items) {
        ReturnForm form = new ReturnForm();
        form.setReturnDate(returnDate);
        form.setItems(List.of(items));
        return form;
    }
}
