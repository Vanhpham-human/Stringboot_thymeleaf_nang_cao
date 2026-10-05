package com.example.library.controller.web;

import com.example.library.dto.BorrowingSearchCriteria;
import com.example.library.entity.enums.BorrowingStatus;
import com.example.library.service.BorrowingService;
import com.example.library.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ReportService reportService;
    private final BorrowingService borrowingService;

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("stats", reportService.dashboard());
        model.addAttribute("topBooks", reportService.topBorrowedBooks());
        model.addAttribute("overdue", reportService.overdueBorrowings(
                PageRequest.of(0, 5, Sort.by("dueDate").ascending())).getContent());
        model.addAttribute("recentBorrowings", borrowingService.search(BorrowingSearchCriteria.empty(),
                PageRequest.of(0, 5, Sort.by("createdAt").descending())).getContent());
        model.addAttribute("statuses", BorrowingStatus.values());
        return "dashboard";
    }
}
