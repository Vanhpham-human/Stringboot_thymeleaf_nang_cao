package com.example.library.repository;

import com.example.library.dto.report.TopBook;
import com.example.library.entity.BorrowingDetail;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BorrowingDetailRepository extends JpaRepository<BorrowingDetail, Long> {

    @Query("""
            SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END
            FROM BorrowingDetail d
            WHERE d.book.id = :bookId AND d.returnedQuantity < d.quantity
              AND d.borrowing.status IN\s""" + BorrowingRepository.OPEN_STATUSES)
    boolean existsUnreturnedByBookId(@Param("bookId") Long bookId);

    @Query("""
            SELECT new com.example.library.dto.report.TopBook(
                b.id, b.isbn, b.title, b.author, c.name,
                COUNT(DISTINCT d.borrowing.id), SUM(d.quantity))
            FROM BorrowingDetail d
            JOIN d.book b
            JOIN b.category c
            WHERE d.borrowing.status <> com.example.library.entity.enums.BorrowingStatus.CANCELLED
            GROUP BY b.id, b.isbn, b.title, b.author, c.name
            ORDER BY SUM(d.quantity) DESC, COUNT(DISTINCT d.borrowing.id) DESC, b.title ASC
            """)
    List<TopBook> findTopBorrowedBooks(Pageable pageable);
}
