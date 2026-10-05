package com.example.library.service;

import com.example.library.dto.BookDto;
import com.example.library.dto.form.BookForm;
import com.example.library.entity.Book;
import com.example.library.entity.Category;
import com.example.library.entity.enums.BookStatus;
import com.example.library.exception.BusinessException;
import com.example.library.exception.DuplicateResourceException;
import com.example.library.exception.ResourceNotFoundException;
import com.example.library.mapper.BookMapper;
import com.example.library.mapper.MapperUtils;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BorrowingDetailRepository;
import com.example.library.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final BorrowingDetailRepository borrowingDetailRepository;
    private final BookMapper bookMapper;

    public Page<BookDto> search(String keyword, Long categoryId, BookStatus status, Pageable pageable) {
        return bookRepository.search(MapperUtils.trimToNull(keyword), categoryId, status, pageable)
                .map(bookMapper::toDto);
    }

    /** Sách có thể chọn khi lập phiếu mượn: đang lưu hành và còn ít nhất 1 cuốn. */
    public List<BookDto> findBorrowable() {
        return bookRepository.findByStatusAndAvailableQuantityGreaterThanOrderByTitleAsc(BookStatus.ACTIVE, 0)
                .stream()
                .map(bookMapper::toDto)
                .toList();
    }

    public BookDto getById(Long id) {
        return bookMapper.toDto(findBook(id));
    }

    public BookForm getForm(Long id) {
        Book book = findBook(id);
        if (book.getStatus() == BookStatus.DELETED) {
            throw new BusinessException("Sách \"" + book.getTitle() + "\" đã bị xóa, không thể chỉnh sửa");
        }
        return bookMapper.toForm(book);
    }

    @Transactional
    public Long create(BookForm form) {
        String isbn = form.getIsbn().trim();
        if (bookRepository.existsByIsbn(isbn)) {
            throw duplicateIsbn(isbn);
        }
        rejectDeletedStatus(form);
        if (form.getAvailableQuantity() > form.getTotalQuantity()) {
            throw new BusinessException("availableQuantity", "Số lượng đang có không được lớn hơn tổng số lượng");
        }

        Book book = new Book();
        bookMapper.updateEntity(form, book);
        book.setCategory(findActiveCategory(form.getCategoryId()));
        book.setTotalQuantity(form.getTotalQuantity());
        book.setAvailableQuantity(form.getAvailableQuantity());
        return bookRepository.save(book).getId();
    }

    /**
     * Khi đổi tổng số lượng, số lượng đang có được tính lại để giữ nguyên số cuốn đang cho mượn,
     * nên giá trị availableQuantity người dùng gửi lên khi sửa sẽ bị bỏ qua.
     */
    @Transactional
    public void update(Long id, BookForm form) {
        Book book = findBook(id);
        if (book.getStatus() == BookStatus.DELETED) {
            throw new BusinessException("Sách \"" + book.getTitle() + "\" đã bị xóa, không thể chỉnh sửa");
        }
        String isbn = form.getIsbn().trim();
        if (bookRepository.existsByIsbnAndIdNot(isbn, id)) {
            throw duplicateIsbn(isbn);
        }
        rejectDeletedStatus(form);

        int borrowed = book.getBorrowedQuantity();
        if (form.getTotalQuantity() < borrowed) {
            throw new BusinessException("totalQuantity",
                    "Tổng số lượng không được nhỏ hơn số cuốn đang cho mượn (" + borrowed + ")");
        }

        if (!book.getCategory().getId().equals(form.getCategoryId())) {
            book.setCategory(findActiveCategory(form.getCategoryId()));
        }
        bookMapper.updateEntity(form, book);
        book.setTotalQuantity(form.getTotalQuantity());
        book.setAvailableQuantity(form.getTotalQuantity() - borrowed);
    }

    /** Xóa mềm (status = DELETED). Không cho xóa nếu sách còn lượt mượn chưa trả. */
    @Transactional
    public void delete(Long id) {
        Book book = findBook(id);
        if (book.getStatus() == BookStatus.DELETED) {
            throw new BusinessException("Sách \"" + book.getTitle() + "\" đã bị xóa trước đó");
        }
        if (borrowingDetailRepository.existsUnreturnedByBookId(id)) {
            throw new BusinessException("Không thể xóa sách \"" + book.getTitle()
                    + "\" vì đang có lượt mượn chưa trả");
        }
        book.setStatus(BookStatus.DELETED);
    }

    Book findBook(Long id) {
        return bookRepository.findWithCategoryById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("sách", id));
    }

    private Category findActiveCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException("categoryId", "Thể loại không tồn tại"));
        if (!category.isActive()) {
            throw new BusinessException("categoryId",
                    "Thể loại \"" + category.getName() + "\" đã ngừng hoạt động, vui lòng chọn thể loại khác");
        }
        return category;
    }

    private void rejectDeletedStatus(BookForm form) {
        if (form.getStatus() == BookStatus.DELETED) {
            throw new BusinessException("status", "Không thể chọn trạng thái Đã xóa trên form, hãy dùng chức năng xóa");
        }
    }

    private DuplicateResourceException duplicateIsbn(String isbn) {
        return new DuplicateResourceException("isbn", "ISBN " + isbn + " đã tồn tại trong hệ thống");
    }
}
