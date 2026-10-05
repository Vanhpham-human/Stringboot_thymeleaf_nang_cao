package com.example.library.dto.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnItemForm {

    @NotNull
    private Long detailId;

    @NotNull(message = "Số lượng trả không được để trống")
    @Min(value = 0, message = "Số lượng trả không được âm")
    private Integer quantity = 0;
}
