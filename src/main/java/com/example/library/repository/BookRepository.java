package com.example.library.repository;

import com.example.library.entity.Book;
import com.example.library.entity.enums.BookStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, Long id);

    @EntityGraph(attributePaths = "category")
    Optional<Book> findWithCategoryById(Long id);

    @EntityGraph(attributePaths = "category")
    @Query(value = """
            SELECT b FROM Book b
            WHERE b.status <> com.example.library.entity.enums.BookStatus.DELETED
              AND (:status IS NULL OR b.status = :status)
              AND (:categoryId IS NULL OR b.category.id = :categoryId)
              AND (:keyword IS NULL
                   OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR b.isbn LIKE CONCAT('%', :keyword, '%')
                   OR LOWER(b.category.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """,
            countQuery = """
            SELECT COUNT(b) FROM Book b
            WHERE b.status <> com.example.library.entity.enums.BookStatus.DELETED
              AND (:status IS NULL OR b.status = :status)
              AND (:categoryId IS NULL OR b.category.id = :categoryId)
              AND (:keyword IS NULL
                   OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR b.isbn LIKE CONCAT('%', :keyword, '%')
                   OR LOWER(b.category.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Book> search(@Param("keyword") String keyword,
                      @Param("categoryId") Long categoryId,
                      @Param("status") BookStatus status,
                      Pageable pageable);

    @EntityGraph(attributePaths = "category")
    List<Book> findByStatusAndAvailableQuantityGreaterThanOrderByTitleAsc(BookStatus status, int availableQuantity);

    long countByStatusNot(BookStatus status);

    @Query("SELECT COALESCE(SUM(b.totalQuantity), 0) FROM Book b WHERE b.status <> com.example.library.entity.enums.BookStatus.DELETED")
    long sumTotalQuantity();

    @Query("SELECT COALESCE(SUM(b.availableQuantity), 0) FROM Book b WHERE b.status <> com.example.library.entity.enums.BookStatus.DELETED")
    long sumAvailableQuantity();
}
