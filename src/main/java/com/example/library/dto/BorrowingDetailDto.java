package com.example.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowingDetailDto {

    private Long id;
    private Long bookId;
    private String isbn;
    private String bookTitle;
    private String author;
    private int quantity;
    private int returnedQuantity;
    private BigDecimal fineAmount;
    private LocalDate lastReturnedDate;
    private int lateDays;

    /** Phí nếu trả hết số còn lại của dòng này vào hôm nay. */
    private BigDecimal pendingFine;

    public int getRemainingQuantity() {
        return quantity - returnedQuantity;
    }

    public boolean isFullyReturned() {
        return returnedQuantity >= quantity;
    }
}
