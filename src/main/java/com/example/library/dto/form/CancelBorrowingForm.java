package com.example.library.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelBorrowingForm {

    @NotBlank(message = "Vui lòng nhập lý do hủy phiếu")
    @Size(max = 255, message = "Lý do hủy tối đa 255 ký tự")
    private String reason;
}
