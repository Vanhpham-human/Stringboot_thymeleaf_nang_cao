package com.example.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FineWaiverDto {

    private Long id;
    private Long borrowingId;
    private BigDecimal amount;
    private String reason;
    private String approvedBy;
    private LocalDate approvedDate;
}
