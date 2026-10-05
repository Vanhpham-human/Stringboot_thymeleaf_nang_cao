package com.example.library.controller.api;

import com.example.library.dto.BorrowingDto;
import com.example.library.dto.BorrowingSearchCriteria;
import com.example.library.dto.PageResponse;
import com.example.library.dto.form.BorrowingForm;
import com.example.library.dto.form.CancelBorrowingForm;
import com.example.library.dto.form.FinePaymentForm;
import com.example.library.dto.form.FineWaiverForm;
import com.example.library.dto.form.ReturnForm;
import com.example.library.entity.enums.BorrowingStatus;
import com.example.library.service.BorrowingService;
import com.example.library.service.FinePaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Borrowings", description = "Mượn và trả sách")
@RestController
@RequestMapping("/api/borrowings")
@RequiredArgsConstructor
public class BorrowingApiController {

    private final BorrowingService borrowingService;
    private final FinePaymentService finePaymentService;

    @Operation(summary = "Danh sách phiếu mượn")
    @GetMapping
    public PageResponse<BorrowingDto> search(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Long memberId,
                                             @RequestParam(required = false) BorrowingStatus status,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                             @RequestParam(defaultValue = "false") boolean overdueOnly,
                                             @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        BorrowingSearchCriteria criteria = new BorrowingSearchCriteria(keyword, memberId, status, fromDate, toDate, overdueOnly);
        return PageResponse.of(borrowingService.search(criteria, pageable));
    }

    @Operation(summary = "Chi tiết phiếu mượn")
    @GetMapping("/{id}")
    public BorrowingDto get(@PathVariable Long id) {
        return borrowingService.getDetail(id);
    }

    @Operation(summary = "Tạo phiếu mượn (nhiều sách, mặc định hạn trả 14 ngày)")
    @PostMapping
    public ResponseEntity<BorrowingDto> create(@Valid @RequestBody BorrowingForm form) {
        Long id = borrowingService.createBorrowing(form);
        return ResponseEntity.status(HttpStatus.CREATED).body(borrowingService.getDetail(id));
    }

    @Operation(summary = "Trả từng phần: chỉ định số lượng trả cho từng dòng chi tiết")
    @PostMapping("/{id}/return")
    public BorrowingDto returnBooks(@PathVariable Long id, @Valid @RequestBody ReturnForm form) {
        return borrowingService.returnBooks(id, form);
    }

    @Operation(summary = "Trả toàn bộ phiếu")
    @PostMapping("/{id}/return-all")
    public BorrowingDto returnAll(@PathVariable Long id,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate returnDate) {
        return borrowingService.returnAll(id, returnDate);
    }

    @Operation(summary = "Hủy phiếu (chỉ khi chưa trả cuốn nào và chưa phát sinh phí)")
    @PostMapping("/{id}/cancel")
    public BorrowingDto cancel(@PathVariable Long id, @Valid @RequestBody CancelBorrowingForm form) {
        borrowingService.cancel(id, form.getReason());
        return borrowingService.getDetail(id);
    }

    @Operation(summary = "Thanh toán phí phạt (một phần hoặc toàn bộ)")
    @PostMapping("/{id}/payments")
    public BorrowingDto pay(@PathVariable Long id, @Valid @RequestBody FinePaymentForm form) {
        finePaymentService.recordPayment(id, form);
        return borrowingService.getDetail(id);
    }

    @Operation(summary = "Miễn giảm phí phạt (bắt buộc lý do và người duyệt)")
    @PostMapping("/{id}/waivers")
    public BorrowingDto waive(@PathVariable Long id, @Valid @RequestBody FineWaiverForm form) {
        finePaymentService.recordWaiver(id, form);
        return borrowingService.getDetail(id);
    }
}
