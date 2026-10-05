package com.example.library.mapper;

import com.example.library.dto.FinePaymentDto;
import com.example.library.dto.FinePolicyDto;
import com.example.library.dto.FineWaiverDto;
import com.example.library.entity.FinePayment;
import com.example.library.entity.FinePolicy;
import com.example.library.entity.FineWaiver;
import org.springframework.stereotype.Component;

@Component
public class FineMapper {

    public FinePolicyDto toDto(FinePolicy policy) {
        return FinePolicyDto.builder()
                .id(policy.getId())
                .name(policy.getName())
                .dailyFineAmount(policy.getDailyFineAmount())
                .maxFineAmount(policy.getMaxFineAmount())
                .graceDays(policy.getGraceDays())
                .active(policy.isActive())
                .description(policy.getDescription())
                .createdAt(policy.getCreatedAt())
                .build();
    }

    public FinePaymentDto toDto(FinePayment payment) {
        return FinePaymentDto.builder()
                .id(payment.getId())
                .borrowingId(payment.getBorrowing().getId())
                .amount(payment.getAmount())
                .paymentDate(payment.getPaymentDate())
                .method(payment.getMethod())
                .note(payment.getNote())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    public FineWaiverDto toDto(FineWaiver waiver) {
        return FineWaiverDto.builder()
                .id(waiver.getId())
                .borrowingId(waiver.getBorrowing().getId())
                .amount(waiver.getAmount())
                .reason(waiver.getReason())
                .approvedBy(waiver.getApprovedBy())
                .approvedDate(waiver.getApprovedDate())
                .build();
    }
}
