package com.example.library.mapper;

import com.example.library.dto.BookDto;
import com.example.library.dto.form.BookForm;
import com.example.library.entity.Book;
import org.springframework.stereotype.Component;

@Component
public class BookMapper {

    public BookDto toDto(Book book) {
        return BookDto.builder()
                .id(book.getId())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .author(book.getAuthor())
                .categoryId(book.getCategory().getId())
                .categoryName(book.getCategory().getName())
                .totalQuantity(book.getTotalQuantity())
                .availableQuantity(book.getAvailableQuantity())
                .status(book.getStatus())
                .createdAt(book.getCreatedAt())
                .build();
    }

    public BookForm toForm(Book book) {
        return BookForm.builder()
                .id(book.getId())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .author(book.getAuthor())
                .categoryId(book.getCategory().getId())
                .totalQuantity(book.getTotalQuantity())
                .availableQuantity(book.getAvailableQuantity())
                .status(book.getStatus())
                .build();
    }

    /** Chỉ map thông tin mô tả; số lượng và thể loại do service xử lý vì có ràng buộc nghiệp vụ. */
    public void updateEntity(BookForm form, Book book) {
        book.setIsbn(form.getIsbn().trim());
        book.setTitle(form.getTitle().trim());
        book.setAuthor(form.getAuthor().trim());
        book.setStatus(form.getStatus());
    }
}
