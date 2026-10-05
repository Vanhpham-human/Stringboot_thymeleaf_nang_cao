package com.example.library.dto.form;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.example.library.entity.enums.BookStatus;
import com.example.library.validation.ValidBookQuantity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidBookQuantity
public class BookForm {

    private Long id;

    @NotBlank(message = "ISBN không được để trống")
    @Pattern(regexp = "^(\\d{9}[\\dX]|\\d{13})$",
            message = "ISBN phải gồm 10 ký tự (9 số + số/X) hoặc 13 chữ số, không chứa dấu gạch")
    private String isbn;

    @NotBlank(message = "Tên sách không được để trống")
    @Size(max = 255, message = "Tên sách tối đa 255 ký tự")
    private String title;

    @NotBlank(message = "Tác giả không được để trống")
    @Size(max = 150, message = "Tên tác giả tối đa 150 ký tự")
    private String author;

    @NotNull(message = "Vui lòng chọn thể loại")
    private Long categoryId;

    @NotNull(message = "Tổng số lượng không được để trống")
    @Min(value = 1, message = "Tổng số lượng phải lớn hơn hoặc bằng 1")
    @Max(value = 10000, message = "Tổng số lượng tối đa 10.000")
    private Integer totalQuantity;

    @NotNull(message = "Số lượng đang có không được để trống")
    @Min(value = 0, message = "Số lượng đang có không được âm")
    private Integer availableQuantity;

    @NotNull(message = "Vui lòng chọn trạng thái")
    @Builder.Default
    private BookStatus status = BookStatus.ACTIVE;

    @JsonIgnore
    public boolean isNew() {
        return id == null;
    }
}
