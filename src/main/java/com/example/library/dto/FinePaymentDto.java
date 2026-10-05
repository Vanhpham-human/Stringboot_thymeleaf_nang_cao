package com.example.library.dto;

import com.example.library.entity.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinePaymentDto {

    private Long id;
    private Long borrowingId;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private PaymentMethod method;
    private String note;
    private LocalDateTime createdAt;
}
