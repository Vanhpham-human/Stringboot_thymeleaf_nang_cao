package com.example.library.dto.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
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
public class FineWaiverForm {

    @NotNull(message = "Số tiền miễn giảm không được để trống")
    @DecimalMin(value = "1", message = "Số tiền miễn giảm phải lớn hơn 0")
    @Digits(integer = 10, fraction = 0, message = "Số tiền miễn giảm phải là số nguyên (VND)")
    private BigDecimal amount;

    @NotBlank(message = "Lý do miễn giảm là bắt buộc")
    @Size(min = 10, max = 500, message = "Lý do miễn giảm phải từ 10 đến 500 ký tự")
    private String reason;

    @NotBlank(message = "Người duyệt là bắt buộc")
    @Size(max = 100, message = "Tên người duyệt tối đa 100 ký tự")
    private String approvedBy;

    @NotNull(message = "Ngày duyệt không được để trống")
    @PastOrPresent(message = "Ngày duyệt không được ở tương lai")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate approvedDate;
}
