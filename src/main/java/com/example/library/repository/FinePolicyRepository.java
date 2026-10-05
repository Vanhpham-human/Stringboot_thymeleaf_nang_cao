package com.example.library.repository;

import com.example.library.entity.FinePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FinePolicyRepository extends JpaRepository<FinePolicy, Long> {

    Optional<FinePolicy> findFirstByActiveTrue();

    List<FinePolicy> findAllByOrderByActiveDescCreatedAtDesc();

    /** Gỡ active của mọi policy trước khi kích hoạt policy mới (DB có UNIQUE chặn hai policy active). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE FinePolicy p SET p.active = false WHERE p.active = true")
    int deactivateAll();
}
