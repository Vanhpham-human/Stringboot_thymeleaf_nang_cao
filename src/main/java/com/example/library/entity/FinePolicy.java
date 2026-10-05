package com.example.library.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Chính sách phí phạt. Mỗi lần đổi mức phí tạo một policy mới và kích hoạt nó,
 * policy cũ được giữ lại để tra cứu. Tại một thời điểm chỉ có một policy active.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "fine_policies")
public class FinePolicy extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "daily_fine_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal dailyFineAmount;

    /** Mức trần phí cho một dòng sách; null = không giới hạn. */
    @Column(name = "max_fine_amount", precision = 12, scale = 2)
    private BigDecimal maxFineAmount;

    /** Số ngày trễ được miễn phạt. */
    @Column(name = "grace_days", nullable = false)
    private int graceDays;

    @Column(nullable = false)
    private boolean active;

    @Column(length = 500)
    private String description;

    public FinePolicy(String name, BigDecimal dailyFineAmount, BigDecimal maxFineAmount, int graceDays) {
        this.name = name;
        this.dailyFineAmount = dailyFineAmount;
        this.maxFineAmount = maxFineAmount;
        this.graceDays = graceDays;
    }
}
