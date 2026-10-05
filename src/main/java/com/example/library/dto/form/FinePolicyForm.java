package com.example.library.dto.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinePolicyForm {

    @NotBlank(message = "Tên chính sách không được để trống")
    @Size(max = 100, message = "Tên chính sách tối đa 100 ký tự")
    private String name;

    @NotNull(message = "Phí mỗi ngày không được để trống")
    @DecimalMin(value = "1", message = "Phí mỗi ngày phải lớn hơn 0")
    @Digits(integer = 10, fraction = 0, message = "Phí mỗi ngày phải là số nguyên (VND)")
    private BigDecimal dailyFineAmount;

    /** Để trống = không giới hạn mức trần. */
    @DecimalMin(value = "1", message = "Mức trần phải lớn hơn 0")
    @Digits(integer = 10, fraction = 0, message = "Mức trần phải là số nguyên (VND)")
    private BigDecimal maxFineAmount;

    @NotNull(message = "Số ngày miễn phạt không được để trống")
    @Min(value = 0, message = "Số ngày miễn phạt không được âm")
    @Max(value = 30, message = "Số ngày miễn phạt tối đa 30 ngày")
    @Builder.Default
    private Integer graceDays = 0;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    private String description;

    /** Kích hoạt ngay sau khi tạo (policy đang active sẽ bị tắt). */
    @Builder.Default
    private boolean activate = true;
}
