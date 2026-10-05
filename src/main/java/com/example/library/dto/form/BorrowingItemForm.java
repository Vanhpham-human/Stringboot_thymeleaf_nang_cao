package com.example.library.dto.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BorrowingItemForm {

    @NotNull(message = "Vui lòng chọn sách")
    private Long bookId;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng mượn tối thiểu là 1")
    @Max(value = 10, message = "Mỗi đầu sách mượn tối đa 10 cuốn")
    private Integer quantity = 1;
}
