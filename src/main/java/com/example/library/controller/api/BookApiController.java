package com.example.library.controller.api;

import com.example.library.dto.BookDto;
import com.example.library.dto.PageResponse;
import com.example.library.dto.form.BookForm;
import com.example.library.entity.enums.BookStatus;
import com.example.library.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Books", description = "Quản lý sách")
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookApiController {

    private final BookService bookService;

    @Operation(summary = "Tìm kiếm sách theo tên, tác giả, ISBN, thể loại (có phân trang)")
    @GetMapping
    public PageResponse<BookDto> search(@RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) Long categoryId,
                                        @RequestParam(required = false) BookStatus status,
                                        @ParameterObject @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.of(bookService.search(keyword, categoryId, status, pageable));
    }

    @Operation(summary = "Xem chi tiết sách")
    @GetMapping("/{id}")
    public BookDto get(@PathVariable Long id) {
        return bookService.getById(id);
    }

    @Operation(summary = "Tạo sách mới")
    @PostMapping
    public ResponseEntity<BookDto> create(@Valid @RequestBody BookForm form) {
        Long id = bookService.create(form);
        return ResponseEntity.status(HttpStatus.CREATED).body(bookService.getById(id));
    }

    @Operation(summary = "Cập nhật sách")
    @PutMapping("/{id}")
    public BookDto update(@PathVariable Long id, @Valid @RequestBody BookForm form) {
        bookService.update(id, form);
        return bookService.getById(id);
    }

    @Operation(summary = "Xóa mềm sách (không cho xóa nếu còn lượt mượn chưa trả)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
