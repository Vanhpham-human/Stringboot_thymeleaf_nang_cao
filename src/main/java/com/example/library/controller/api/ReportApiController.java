package com.example.library.controller.api;

import com.example.library.dto.BorrowingDto;
import com.example.library.dto.PageResponse;
import com.example.library.dto.report.DashboardStats;
import com.example.library.dto.report.MemberBorrowStat;
import com.example.library.dto.report.TopBook;
import com.example.library.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Reports", description = "Báo cáo")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportApiController {

    private final ReportService reportService;

    @Operation(summary = "Phiếu mượn quá hạn (chưa trả hết)")
    @GetMapping("/overdue")
    public PageResponse<BorrowingDto> overdue(@ParameterObject @PageableDefault(sort = "dueDate") Pageable pageable) {
        return PageResponse.of(reportService.overdueBorrowings(pageable));
    }

    @Operation(summary = "Top 5 sách được mượn nhiều nhất")
    @GetMapping("/top-books")
    public List<TopBook> topBooks() {
        return reportService.topBorrowedBooks();
    }

    @Operation(summary = "Số lượt mượn theo thành viên")
    @GetMapping("/member-stats")
    public PageResponse<MemberBorrowStat> memberStats(@ParameterObject @PageableDefault Pageable pageable) {
        return PageResponse.of(reportService.memberBorrowStats(pageable));
    }

    @Operation(summary = "Số liệu tổng quan")
    @GetMapping("/dashboard")
    public DashboardStats dashboard() {
        return reportService.dashboard();
    }
}
