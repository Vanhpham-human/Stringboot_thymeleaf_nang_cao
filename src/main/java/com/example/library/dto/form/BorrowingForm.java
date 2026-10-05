package com.example.library.dto.form;

import com.example.library.validation.ValidBorrowingDates;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidBorrowingDates
public class BorrowingForm {

    @NotNull(message = "Vui lòng chọn thành viên")
    private Long memberId;

    @NotNull(message = "Ngày mượn không được để trống")
    @PastOrPresent(message = "Ngày mượn không được ở tương lai")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate borrowDate;

    /** Để trống thì hệ thống tự tính = ngày mượn + 14 ngày. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String note;

    @Valid
    @NotEmpty(message = "Phiếu mượn phải có ít nhất một cuốn sách")
    @Builder.Default
    private List<BorrowingItemForm> items = new ArrayList<>();
}
