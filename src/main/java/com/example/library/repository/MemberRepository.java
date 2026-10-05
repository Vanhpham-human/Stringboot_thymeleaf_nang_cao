package com.example.library.repository;

import com.example.library.entity.Member;
import com.example.library.entity.enums.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    List<Member> findByStatusOrderByFullNameAsc(MemberStatus status);

    long countByStatus(MemberStatus status);

    @Query("""
            SELECT m FROM Member m
            WHERE (:status IS NULL OR m.status = :status)
              AND (:keyword IS NULL
                   OR LOWER(m.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(m.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR m.phone LIKE CONCAT('%', :keyword, '%'))
            """)
    Page<Member> search(@Param("keyword") String keyword, @Param("status") MemberStatus status, Pageable pageable);
}
