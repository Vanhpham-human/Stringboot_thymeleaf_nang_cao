package com.example.library.service;

import com.example.library.dto.BorrowingDto;
import com.example.library.dto.report.DashboardStats;
import com.example.library.dto.report.MemberBorrowStat;
import com.example.library.dto.report.TopBook;
import com.example.library.entity.FinePolicy;
import com.example.library.entity.enums.BookStatus;
import com.example.library.entity.enums.BorrowingStatus;
import com.example.library.entity.enums.MemberStatus;
import com.example.library.mapper.BorrowingMapper;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BorrowingDetailRepository;
import com.example.library.repository.BorrowingRepository;
import com.example.library.repository.FinePaymentRepository;
import com.example.library.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    public static final int TOP_BOOK_LIMIT = 5;

    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final BorrowingRepository borrowingRepository;
    private final BorrowingDetailRepository borrowingDetailRepository;
    private final FinePaymentRepository finePaymentRepository;
    private final FinePolicyService finePolicyService;
    private final BorrowingMapper borrowingMapper;
    private final Clock clock;

    public DashboardStats dashboard() {
        return new DashboardStats(
                bookRepository.countByStatusNot(BookStatus.DELETED),
                bookRepository.sumTotalQuantity(),
                bookRepository.sumAvailableQuantity(),
                memberRepository.countByStatus(MemberStatus.ACTIVE),
                memberRepository.countByStatus(MemberStatus.BLOCKED),
                borrowingRepository.countOpen(),
                borrowingRepository.countOverdue(today()),
                borrowingRepository.countByStatus(BorrowingStatus.FINE_PENDING),
                borrowingRepository.sumTotalFine(),
                finePaymentRepository.sumAmount(),
                borrowingRepository.sumWaivedFine(),
                borrowingRepository.sumUnpaidFine());
    }

    /** Phiếu chưa trả hết và đã quá hạn trả, kèm số ngày trễ và phí dự kiến theo policy đang áp dụng. */
    public Page<BorrowingDto> overdueBorrowings(Pageable pageable) {
        LocalDate today = today();
        FinePolicy policy = finePolicyService.getActivePolicy();
        return borrowingRepository.findOverdue(today, pageable)
                .map(b -> borrowingMapper.toSummaryDto(b, today, policy));
    }

    public List<TopBook> topBorrowedBooks() {
        return borrowingDetailRepository.findTopBorrowedBooks(PageRequest.of(0, TOP_BOOK_LIMIT));
    }

    /** Thứ tự sắp xếp đã cố định trong câu truy vấn nên bỏ qua tham số sort, chỉ dùng page/size. */
    public Page<MemberBorrowStat> memberBorrowStats(Pageable pageable) {
        return borrowingRepository.memberBorrowStats(
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.unsorted()));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
