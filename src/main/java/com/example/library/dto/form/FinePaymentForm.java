package com.example.library.dto.form;

import com.example.library.entity.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinePaymentForm {

    @NotNull(message = "Số tiền thanh toán không được để trống")
    @DecimalMin(value = "1", message = "Số tiền thanh toán phải lớn hơn 0")
    @Digits(integer = 10, fraction = 0, message = "Số tiền thanh toán phải là số nguyên (VND)")
    private BigDecimal amount;

    @NotNull(message = "Ngày thanh toán không được để trống")
    @PastOrPresent(message = "Ngày thanh toán không được ở tương lai")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate paymentDate;

    @NotNull(message = "Vui lòng chọn hình thức thanh toán")
    @Builder.Default
    private PaymentMethod method = PaymentMethod.CASH;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String note;
}
