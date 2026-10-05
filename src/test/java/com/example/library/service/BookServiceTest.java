package com.example.library.service;

import com.example.library.dto.form.BookForm;
import com.example.library.entity.Book;
import com.example.library.entity.Category;
import com.example.library.entity.enums.BookStatus;
import com.example.library.exception.BusinessException;
import com.example.library.exception.DuplicateResourceException;
import com.example.library.mapper.BookMapper;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BorrowingDetailRepository;
import com.example.library.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private BorrowingDetailRepository borrowingDetailRepository;

    private BookService service;

    @BeforeEach
    void setUp() {
        service = new BookService(bookRepository, categoryRepository, borrowingDetailRepository, new BookMapper());
    }

    @Test
    @DisplayName("Không cho tạo sách trùng ISBN")
    void createRejectsDuplicateIsbn() {
        when(bookRepository.existsByIsbn("9780132350884")).thenReturn(true);

        assertThatThrownBy(() -> service.create(form(null, 5, 5)))
                .isInstanceOf(DuplicateResourceException.class)
                .extracting("field").isEqualTo("isbn");
        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("Không cho chọn thể loại đã ngừng hoạt động")
    void createRejectsInactiveCategory() {
        Category category = category(1L);
        category.setActive(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> service.create(form(null, 5, 5)))
                .isInstanceOf(BusinessException.class)
                .extracting("field").isEqualTo("categoryId");
    }

    @Test
    @DisplayName("Tạo sách hợp lệ")
    void createSavesBook() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L)));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> {
            Book b = inv.getArgument(0);
            b.setId(7L);
            return b;
        });

        Long id = service.create(form(null, 5, 4));

        assertThat(id).isEqualTo(7L);
    }

    @Test
    @DisplayName("Sửa tổng số lượng: tính lại số đang có, giữ nguyên số đang cho mượn")
    void updateRecalculatesAvailable() {
        Book book = book(5, 3);
        when(bookRepository.findWithCategoryById(1L)).thenReturn(Optional.of(book));

        service.update(1L, form(1L, 8, 99));

        assertThat(book.getTotalQuantity()).isEqualTo(8);
        assertThat(book.getAvailableQuantity()).isEqualTo(6);
    }

    @Test
    @DisplayName("Tổng số lượng mới không được nhỏ hơn số đang cho mượn")
    void updateRejectsTotalBelowBorrowed() {
        when(bookRepository.findWithCategoryById(1L)).thenReturn(Optional.of(book(5, 1)));

        assertThatThrownBy(() -> service.update(1L, form(1L, 3, 0)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đang cho mượn (4)");
    }

    @Test
    @DisplayName("Không cho xóa sách đang có lượt mượn chưa trả")
    void deleteRejectsUnreturnedBook() {
        Book book = book(5, 3);
        when(bookRepository.findWithCategoryById(1L)).thenReturn(Optional.of(book));
        when(borrowingDetailRepository.existsUnreturnedByBookId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("chưa trả");
        assertThat(book.getStatus()).isEqualTo(BookStatus.ACTIVE);
    }

    @Test
    @DisplayName("Xóa mềm sách khi không còn lượt mượn")
    void deleteSoftDeletes() {
        Book book = book(5, 5);
        when(bookRepository.findWithCategoryById(1L)).thenReturn(Optional.of(book));
        when(borrowingDetailRepository.existsUnreturnedByBookId(1L)).thenReturn(false);

        service.delete(1L);

        assertThat(book.getStatus()).isEqualTo(BookStatus.DELETED);
    }

    private static BookForm form(Long id, int total, int available) {
        return BookForm.builder()
                .id(id)
                .isbn("9780132350884")
                .title("Clean Code")
                .author("Robert C. Martin")
                .categoryId(1L)
                .totalQuantity(total)
                .availableQuantity(available)
                .status(BookStatus.ACTIVE)
                .build();
    }

    private static Category category(Long id) {
        Category category = new Category();
        category.setId(id);
        category.setName("CNTT");
        category.setActive(true);
        return category;
    }

    private static Book book(int total, int available) {
        Book book = new Book();
        book.setId(1L);
        book.setIsbn("9780132350884");
        book.setTitle("Clean Code");
        book.setAuthor("Robert C. Martin");
        book.setCategory(category(1L));
        book.setTotalQuantity(total);
        book.setAvailableQuantity(available);
        book.setStatus(BookStatus.ACTIVE);
        return book;
    }
}
