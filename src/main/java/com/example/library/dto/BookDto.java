package com.example.library.dto;

import com.example.library.entity.enums.BookStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookDto {

    private Long id;
    private String isbn;
    private String title;
    private String author;
    private Long categoryId;
    private String categoryName;
    private int totalQuantity;
    private int availableQuantity;
    private BookStatus status;
    private LocalDateTime createdAt;

    public int getBorrowedQuantity() {
        return totalQuantity - availableQuantity;
    }
}
