package com.example.library.repository;

import com.example.library.entity.FinePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface FinePaymentRepository extends JpaRepository<FinePayment, Long> {

    List<FinePayment> findByBorrowingIdOrderByPaymentDateDescIdDesc(Long borrowingId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM FinePayment p")
    BigDecimal sumAmount();
}
