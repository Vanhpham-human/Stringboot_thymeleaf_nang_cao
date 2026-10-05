package com.example.library.dto.report;

import lombok.Getter;

@Getter
public class TopBook {

    private final Long bookId;
    private final String isbn;
    private final String title;
    private final String author;
    private final String categoryName;
    private final long borrowingCount;
    private final long totalQuantity;

    public TopBook(Long bookId, String isbn, String title, String author, String categoryName,
                   Number borrowingCount, Number totalQuantity) {
        this.bookId = bookId;
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.categoryName = categoryName;
        this.borrowingCount = borrowingCount == null ? 0 : borrowingCount.longValue();
        this.totalQuantity = totalQuantity == null ? 0 : totalQuantity.longValue();
    }
}
