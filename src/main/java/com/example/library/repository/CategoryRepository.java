package com.example.library.repository;

import com.example.library.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<Category> findByActiveTrueOrderByNameAsc();

    @Query("""
            SELECT c FROM Category c
            WHERE (:active IS NULL OR c.active = :active)
              AND (:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Category> search(@Param("keyword") String keyword, @Param("active") Boolean active, Pageable pageable);

    @Query("""
            SELECT c.id, COUNT(b.id) FROM Category c LEFT JOIN c.books b
              ON b.status <> com.example.library.entity.enums.BookStatus.DELETED
            WHERE c.id IN :ids
            GROUP BY c.id
            """)
    List<Object[]> countBooksByCategoryIds(@Param("ids") List<Long> ids);
}
