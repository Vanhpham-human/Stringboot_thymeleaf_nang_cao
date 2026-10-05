package com.example.library.repository;

import com.example.library.entity.FineWaiver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FineWaiverRepository extends JpaRepository<FineWaiver, Long> {

    List<FineWaiver> findByBorrowingIdOrderByApprovedDateDescIdDesc(Long borrowingId);
}
