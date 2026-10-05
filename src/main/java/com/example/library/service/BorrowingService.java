package com.example.library.service;

import com.example.library.config.BorrowingProperties;
import com.example.library.dto.BorrowingDto;
import com.example.library.dto.BorrowingSearchCriteria;
import com.example.library.dto.MemberBorrowingSummary;
import com.example.library.dto.form.BorrowingForm;
import com.example.library.dto.form.BorrowingItemForm;
import com.example.library.dto.form.ReturnForm;
import com.example.library.dto.form.ReturnItemForm;
import com.example.library.entity.Book;
import com.example.library.entity.Borrowing;
import com.example.library.entity.BorrowingDetail;
import com.example.library.entity.FinePolicy;
import com.example.library.entity.Member;
import com.example.library.entity.enums.BorrowingStatus;
import com.example.library.exception.BusinessException;
import com.example.library.exception.ConcurrentUpdateException;
import com.example.library.exception.ResourceNotFoundException;
import com.example.library.mapper.BorrowingMapper;
import com.example.library.mapper.MapperUtils;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BorrowingRepository;
import com.example.library.repository.MemberRepository;
import com.example.library.service.FineCalculationService.FineResult;
import com.example.library.util.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Nghiệp vụ mượn / trả / hủy phiếu. Mọi thao tác ghi đều chạy trong một transaction:
 * chỉ cần một bước lỗi (sách không đủ, tính phí lỗi, xung đột cập nhật...) là toàn bộ thay đổi
 * (tồn kho, số lượng đã trả, phí phạt, trạng thái) bị rollback cùng nhau.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BorrowingService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BorrowingRepository borrowingRepository;
    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;
    private final BorrowingMapper borrowingMapper;
    private final FineCalculationService fineCalculationService;
    private final FinePolicyService finePolicyService;
    private final BorrowingProperties borrowingProperties;
    private final Clock clock;

    // ------------------------------------------------------------------ Truy vấn

    public Page<BorrowingDto> search(BorrowingSearchCriteria criteria, Pageable pageable) {
        LocalDate today = today();
        FinePolicy policy = finePolicyService.getActivePolicy();
        return borrowingRepository.search(MapperUtils.trimToNull(criteria.keyword()), criteria.memberId(),
                        criteria.status(), criteria.fromDate(), criteria.toDate(), criteria.overdueOnly(), today, pageable)
                .map(b -> borrowingMapper.toSummaryDto(b, today, policy));
    }

    public BorrowingDto getDetail(Long id) {
        return borrowingMapper.toDetailDto(findDetailed(id), today(), finePolicyService.getActivePolicy());
    }

    public MemberBorrowingSummary memberSummary(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> ResourceNotFoundException.of("độc giả", memberId));
        return new MemberBorrowingSummary(
                member.isBlocked(),
                borrowingRepository.countUnreturnedBooksByMember(memberId),
                borrowingProperties.maxBooksPerMember(),
                borrowingRepository.existsOverdueByMember(memberId, today()),
                borrowingRepository.sumUnpaidFineByMember(memberId));
    }

    public BorrowingForm newBorrowingForm() {
        LocalDate today = today();
        BorrowingForm form = new BorrowingForm();
        form.setBorrowDate(today);
        form.setDueDate(today.plusDays(borrowingProperties.defaultLoanDays()));
        form.getItems().add(new BorrowingItemForm(null, 1));
        return form;
    }

    public ReturnForm newReturnForm(Long id) {
        Borrowing borrowing = findDetailed(id);
        ensureReturnable(borrowing);
        ReturnForm form = new ReturnForm();
        form.setReturnDate(today());
        borrowing.getDetails().stream()
                .filter(d -> !d.isFullyReturned())
                .sorted(Comparator.comparing(BorrowingDetail::getId))
                .forEach(d -> form.getItems().add(new ReturnItemForm(d.getId(), 0)));
        return form;
    }

    // ------------------------------------------------------------------ Mượn sách

    /**
     * Tạo phiếu mượn: kiểm tra độc giả (khóa, nợ phí, quá hạn, giới hạn số cuốn), kiểm tra từng dòng sách
     * (trùng, số lượng, trạng thái, tồn kho) rồi trừ tồn kho. Lỗi ở bất kỳ dòng nào -> rollback toàn bộ phiếu.
     */
    @Transactional
    public Long createBorrowing(BorrowingForm form) {
        LocalDate today = today();
        Member member = memberRepository.findById(form.getMemberId())
                .orElseThrow(() -> new BusinessException("memberId", "Độc giả không tồn tại"));
        ensureMemberCanBorrow(member, today);

        LocalDate borrowDate = form.getBorrowDate() != null ? form.getBorrowDate() : today;
        if (borrowDate.isAfter(today)) {
            throw new BusinessException("borrowDate", "Ngày mượn không được ở tương lai");
        }
        LocalDate dueDate = form.getDueDate() != null
                ? form.getDueDate()
                : borrowDate.plusDays(borrowingProperties.defaultLoanDays());
        if (!dueDate.isAfter(borrowDate)) {
            throw new BusinessException("dueDate", "Hạn trả phải sau ngày mượn");
        }

        Map<Long, Integer> quantities = validateItems(form.getItems());
        int requested = quantities.values().stream().mapToInt(Integer::intValue).sum();
        long holding = borrowingRepository.countUnreturnedBooksByMember(member.getId());
        int limit = borrowingProperties.maxBooksPerMember();
        if (holding + requested > limit) {
            throw new BusinessException("items", "Độc giả đang giữ %d cuốn chưa trả, mượn thêm %d cuốn sẽ vượt giới hạn %d cuốn / người"
                    .formatted(holding, requested, limit));
        }

        Borrowing borrowing = new Borrowing();
        borrowing.setMember(member);
        borrowing.setBorrowDate(borrowDate);
        borrowing.setDueDate(dueDate);
        borrowing.setNote(MapperUtils.trimToNull(form.getNote()));

        quantities.forEach((bookId, quantity) -> {
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new BusinessException("items", "Sách có mã " + bookId + " không tồn tại"));
            if (!book.isBorrowable()) {
                throw new BusinessException("items", "Sách \"%s\" đang ở trạng thái %s, không thể cho mượn"
                        .formatted(book.getTitle(), book.getStatus().getLabel()));
            }
            book.decreaseAvailable(quantity);

            BorrowingDetail detail = new BorrowingDetail();
            detail.setBook(book);
            detail.setQuantity(quantity);
            detail.setReturnedQuantity(0);
            detail.setFineAmount(BigDecimal.ZERO);
            borrowing.addDetail(detail);
        });
        borrowing.refreshStatus(today);

        Borrowing saved = flush(borrowing);
        log.info("Tạo phiếu mượn #{} cho độc giả {} với {} cuốn", saved.getId(), member.getEmail(), requested);
        return saved.getId();
    }

    private void ensureMemberCanBorrow(Member member, LocalDate today) {
        if (member.isBlocked()) {
            throw new BusinessException("memberId",
                    "Độc giả " + member.getFullName() + " đang bị khóa, không thể tạo phiếu mượn mới");
        }
        BigDecimal unpaid = borrowingRepository.sumUnpaidFineByMember(member.getId());
        if (unpaid.signum() > 0) {
            throw new BusinessException("memberId", "Độc giả %s còn nợ %s đ phí phạt, cần thanh toán hết trước khi mượn mới"
                    .formatted(member.getFullName(), Money.format(unpaid)));
        }
        if (borrowingRepository.existsOverdueByMember(member.getId(), today)) {
            throw new BusinessException("memberId",
                    "Độc giả " + member.getFullName() + " đang có phiếu mượn quá hạn, cần trả sách trước khi mượn mới");
        }
    }

    /** Mỗi dòng phải có sách, số lượng > 0 và không được trùng bookId với dòng khác. */
    private Map<Long, Integer> validateItems(List<BorrowingItemForm> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("items", "Phiếu mượn phải có ít nhất một đầu sách");
        }
        Set<Long> seen = new HashSet<>();
        for (BorrowingItemForm item : items) {
            if (item.getBookId() == null) {
                throw new BusinessException("items", "Vui lòng chọn sách cho tất cả các dòng");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException("items", "Số lượng mượn phải lớn hơn 0");
            }
            if (!seen.add(item.getBookId())) {
                String title = bookRepository.findById(item.getBookId()).map(Book::getTitle).orElse("#" + item.getBookId());
                throw new BusinessException("items",
                        "Sách \"" + title + "\" bị chọn ở nhiều dòng, mỗi đầu sách chỉ được xuất hiện một lần");
            }
        }
        return items.stream().collect(Collectors.toMap(BorrowingItemForm::getBookId, BorrowingItemForm::getQuantity,
                (a, b) -> a, LinkedHashMap::new));
    }

    // ------------------------------------------------------------------ Trả sách

    /** Trả từng phần: mỗi dòng nhập số lượng trả (0 = không trả dòng đó). Phí chỉ tính trên số cuốn trả lần này. */
    @Transactional
    public BorrowingDto returnBooks(Long id, ReturnForm form) {
        Borrowing borrowing = findDetailed(id);
        ensureReturnable(borrowing);
        LocalDate today = today();
        LocalDate returnDate = form.getReturnDate() != null ? form.getReturnDate() : today;
        validateReturnDate(borrowing, returnDate, today);
        FinePolicy policy = finePolicyService.getActivePolicy();

        Map<Long, BorrowingDetail> detailsById = borrowing.getDetails().stream()
                .collect(Collectors.toMap(BorrowingDetail::getId, Function.identity()));

        int totalReturned = 0;
        for (ReturnItemForm item : form.getItems()) {
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            if (quantity == 0) {
                continue;
            }
            if (quantity < 0) {
                throw new BusinessException("Số lượng trả không được âm");
            }
            BorrowingDetail detail = detailsById.get(item.getDetailId());
            if (detail == null) {
                throw new BusinessException("Dòng sách #" + item.getDetailId() + " không thuộc phiếu mượn này");
            }
            if (quantity > detail.getRemainingQuantity()) {
                throw new BusinessException("Sách \"%s\" chỉ còn %d cuốn đang mượn, không thể trả %d cuốn"
                        .formatted(detail.getBook().getTitle(), detail.getRemainingQuantity(), quantity));
            }
            returnDetail(borrowing, detail, quantity, returnDate, policy);
            totalReturned += quantity;
        }

        if (totalReturned == 0) {
            throw new BusinessException("Vui lòng nhập số lượng trả cho ít nhất một cuốn sách");
        }
        return finishReturn(borrowing, returnDate, today, policy);
    }

    @Transactional
    public BorrowingDto returnAll(Long id, LocalDate returnDate) {
        Borrowing borrowing = findDetailed(id);
        ensureReturnable(borrowing);
        LocalDate today = today();
        LocalDate effectiveDate = returnDate != null ? returnDate : today;
        validateReturnDate(borrowing, effectiveDate, today);
        FinePolicy policy = finePolicyService.getActivePolicy();

        borrowing.getDetails().stream()
                .filter(d -> !d.isFullyReturned())
                .forEach(d -> returnDetail(borrowing, d, d.getRemainingQuantity(), effectiveDate, policy));
        return finishReturn(borrowing, effectiveDate, today, policy);
    }

    private void returnDetail(Borrowing borrowing, BorrowingDetail detail, int quantity, LocalDate returnDate,
                              FinePolicy policy) {
        detail.getBook().increaseAvailable(quantity);
        FineResult fine = fineCalculationService.calculate(borrowing.getDueDate(), returnDate, quantity,
                detail.getFineAmount(), policy);
        detail.setReturnedQuantity(detail.getReturnedQuantity() + quantity);
        detail.setLastReturnedDate(returnDate);
        detail.setLateDays((int) fine.lateDays());
        detail.setFineAmount(detail.getFineAmount().add(fine.amount()));
        borrowing.addFine(fine.amount());
    }

    private BorrowingDto finishReturn(Borrowing borrowing, LocalDate returnDate, LocalDate today, FinePolicy policy) {
        if (borrowing.isFullyReturned()) {
            borrowing.setReturnedDate(returnDate);
        }
        borrowing.refreshStatus(today);
        Borrowing saved = flush(borrowing);
        log.info("Trả sách phiếu #{}: trạng thái {}, tổng phí {}, còn nợ {}", saved.getId(), saved.getStatus(),
                saved.getTotalFineAmount(), saved.getUnpaidFineAmount());
        return borrowingMapper.toDetailDto(saved, today, policy);
    }

    // ------------------------------------------------------------------ Hủy phiếu

    /** Chỉ hủy được phiếu chưa trả cuốn nào và chưa phát sinh phí; hoàn lại toàn bộ tồn kho. */
    @Transactional
    public void cancel(Long id, String reason) {
        Borrowing borrowing = findDetailed(id);
        if (borrowing.getStatus() == BorrowingStatus.CANCELLED) {
            throw new BusinessException("Phiếu mượn #" + id + " đã bị hủy trước đó");
        }
        if (!borrowing.isCancellable()) {
            throw new BusinessException("Chỉ được hủy phiếu chưa trả cuốn nào và chưa phát sinh phí phạt");
        }
        String trimmedReason = MapperUtils.trimToNull(reason);
        if (trimmedReason == null) {
            throw new BusinessException("reason", "Vui lòng nhập lý do hủy phiếu");
        }
        borrowing.getDetails().forEach(d -> d.getBook().increaseAvailable(d.getQuantity()));
        borrowing.setStatus(BorrowingStatus.CANCELLED);
        borrowing.setCancelledDate(today());
        borrowing.setCancelReason(trimmedReason);
        flush(borrowing);
        log.info("Hủy phiếu mượn #{}: {}", id, trimmedReason);
    }

    /** Chuyển các phiếu còn sách chưa trả đã quá hạn sang OVERDUE. Được gọi bởi scheduled job. */
    @Transactional
    public int markOverdueBorrowings() {
        return borrowingRepository.markOverdue(today(), LocalDateTime.now(clock));
    }

    // ------------------------------------------------------------------ Tiện ích

    /**
     * Flush ngay trong transaction để phát hiện xung đột @Version (hai người cùng mượn / trả một đầu sách)
     * và trả về thông báo dễ hiểu; transaction vẫn bị rollback toàn bộ.
     */
    private Borrowing flush(Borrowing borrowing) {
        try {
            return borrowingRepository.saveAndFlush(borrowing);
        } catch (OptimisticLockingFailureException ex) {
            throw new ConcurrentUpdateException(
                    "Số lượng sách vừa được cập nhật bởi một giao dịch khác. Vui lòng tải lại trang và thử lại.");
        }
    }

    private void validateReturnDate(Borrowing borrowing, LocalDate returnDate, LocalDate today) {
        if (returnDate.isAfter(today)) {
            throw new BusinessException("returnDate", "Ngày trả không được ở tương lai");
        }
        if (returnDate.isBefore(borrowing.getBorrowDate())) {
            throw new BusinessException("returnDate",
                    "Ngày trả không được trước ngày mượn (" + borrowing.getBorrowDate().format(DATE_FORMAT) + ")");
        }
    }

    private void ensureReturnable(Borrowing borrowing) {
        if (borrowing.getStatus() == BorrowingStatus.CANCELLED) {
            throw new BusinessException("Phiếu mượn #" + borrowing.getId() + " đã bị hủy, không thể trả sách");
        }
        if (!borrowing.isOpen()) {
            throw new BusinessException("Phiếu mượn #" + borrowing.getId() + " đã được trả đủ sách");
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
