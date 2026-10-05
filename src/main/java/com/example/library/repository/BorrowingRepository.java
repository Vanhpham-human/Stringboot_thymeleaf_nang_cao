package com.example.library.repository;

import com.example.library.dto.report.MemberBorrowStat;
import com.example.library.entity.Borrowing;
import com.example.library.entity.enums.BorrowingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public interface BorrowingRepository extends JpaRepository<Borrowing, Long> {

    /** Trạng thái phiếu còn sách chưa trả (đồng bộ với {@link BorrowingStatus#OPEN_STATUSES}). */
    String OPEN_STATUSES = "(com.example.library.entity.enums.BorrowingStatus.BORROWING, "
            + "com.example.library.entity.enums.BorrowingStatus.PARTIALLY_RETURNED, "
            + "com.example.library.entity.enums.BorrowingStatus.OVERDUE)";

    String SEARCH_FILTER = """
            WHERE (:status IS NULL OR br.status = :status)
              AND (:memberId IS NULL OR br.member.id = :memberId)
              AND (:fromDate IS NULL OR br.borrowDate >= :fromDate)
              AND (:toDate IS NULL OR br.borrowDate <= :toDate)
              AND (:overdueOnly = false OR (br.dueDate < :today AND br.status IN\s""" + OPEN_STATUSES + """
            ))
              AND (:keyword IS NULL
                   OR LOWER(br.member.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(br.member.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """;

    @EntityGraph(attributePaths = {"member", "details", "details.book"})
    Optional<Borrowing> findDetailedById(Long id);

    @EntityGraph(attributePaths = "member")
    @Query(value = "SELECT br FROM Borrowing br " + SEARCH_FILTER,
            countQuery = "SELECT COUNT(br) FROM Borrowing br " + SEARCH_FILTER)
    Page<Borrowing> search(@Param("keyword") String keyword,
                           @Param("memberId") Long memberId,
                           @Param("status") BorrowingStatus status,
                           @Param("fromDate") LocalDate fromDate,
                           @Param("toDate") LocalDate toDate,
                           @Param("overdueOnly") boolean overdueOnly,
                           @Param("today") LocalDate today,
                           Pageable pageable);

    @EntityGraph(attributePaths = "member")
    @Query(value = "SELECT br FROM Borrowing br WHERE br.dueDate < :today AND br.status IN " + OPEN_STATUSES,
            countQuery = "SELECT COUNT(br) FROM Borrowing br WHERE br.dueDate < :today AND br.status IN " + OPEN_STATUSES)
    Page<Borrowing> findOverdue(@Param("today") LocalDate today, Pageable pageable);

    @Query("SELECT COUNT(br) FROM Borrowing br WHERE br.dueDate < :today AND br.status IN " + OPEN_STATUSES)
    long countOverdue(@Param("today") LocalDate today);

    @Query("SELECT COUNT(br) FROM Borrowing br WHERE br.status IN " + OPEN_STATUSES)
    long countOpen();

    long countByStatus(BorrowingStatus status);

    // ---------- Quy tắc chặn mượn theo độc giả ----------

    /** Tổng số cuốn độc giả đang giữ (chưa trả) trên các phiếu còn mở. */
    @Query("""
            SELECT COALESCE(SUM(d.quantity - d.returnedQuantity), 0)
            FROM BorrowingDetail d
            WHERE d.borrowing.member.id = :memberId AND d.borrowing.status IN\s""" + OPEN_STATUSES)
    long countUnreturnedBooksByMember(@Param("memberId") Long memberId);

    @Query("""
            SELECT COUNT(br) > 0 FROM Borrowing br
            WHERE br.member.id = :memberId AND br.dueDate < :today AND br.status IN\s""" + OPEN_STATUSES)
    boolean existsOverdueByMember(@Param("memberId") Long memberId, @Param("today") LocalDate today);

    @Query("SELECT COALESCE(SUM(br.unpaidFineAmount), 0) FROM Borrowing br WHERE br.member.id = :memberId")
    BigDecimal sumUnpaidFineByMember(@Param("memberId") Long memberId);

    // ---------- Thống kê ----------

    @Query("SELECT COALESCE(SUM(br.totalFineAmount), 0) FROM Borrowing br")
    BigDecimal sumTotalFine();

    @Query("SELECT COALESCE(SUM(br.unpaidFineAmount), 0) FROM Borrowing br")
    BigDecimal sumUnpaidFine();

    @Query("SELECT COALESCE(SUM(br.waivedFineAmount), 0) FROM Borrowing br")
    BigDecimal sumWaivedFine();

    /** Dùng cho scheduled job: phiếu còn sách chưa trả và đã qua hạn -> OVERDUE. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Borrowing br SET br.status = com.example.library.entity.enums.BorrowingStatus.OVERDUE,
                                    br.updatedAt = :now,
                                    br.version = br.version + 1
            WHERE br.status IN (com.example.library.entity.enums.BorrowingStatus.BORROWING,
                                com.example.library.entity.enums.BorrowingStatus.PARTIALLY_RETURNED)
              AND br.dueDate < :today
            """)
    int markOverdue(@Param("today") LocalDate today, @Param("now") LocalDateTime now);

    @Query(value = """
            SELECT new com.example.library.dto.report.MemberBorrowStat(
                m.id, m.fullName, m.email, m.status,
                COUNT(DISTINCT br.id),
                COALESCE(SUM(d.quantity), 0),
                COALESCE(SUM(CASE WHEN br.status IN\s""" + OPEN_STATUSES + """
             THEN d.quantity - d.returnedQuantity ELSE 0 END), 0),
                COALESCE(SUM(d.fineAmount), 0))
            FROM Member m
            LEFT JOIN m.borrowings br ON br.status <> com.example.library.entity.enums.BorrowingStatus.CANCELLED
            LEFT JOIN br.details d
            GROUP BY m.id, m.fullName, m.email, m.status
            ORDER BY COUNT(DISTINCT br.id) DESC, m.fullName ASC
            """,
            countQuery = "SELECT COUNT(m) FROM Member m")
    Page<MemberBorrowStat> memberBorrowStats(Pageable pageable);
}
