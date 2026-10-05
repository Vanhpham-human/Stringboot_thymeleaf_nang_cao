package com.example.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinePolicyDto {

    private Long id;
    private String name;
    private BigDecimal dailyFineAmount;
    private BigDecimal maxFineAmount;
    private int graceDays;
    private boolean active;
    private String description;
    private LocalDateTime createdAt;

    /** Policy dự phòng lấy từ application.yml (chưa lưu DB). */
    public boolean isFallback() {
        return id == null;
    }
}
