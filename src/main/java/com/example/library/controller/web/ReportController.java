package com.example.library.controller.web;

import com.example.library.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    public String index() {
        return "redirect:/reports/overdue";
    }

    @GetMapping("/overdue")
    public String overdue(@PageableDefault(sort = "dueDate", direction = Sort.Direction.ASC) Pageable pageable, Model model) {
        model.addAttribute("page", reportService.overdueBorrowings(pageable));
        model.addAttribute("activeTab", "overdue");
        return "reports/overdue";
    }

    @GetMapping("/top-books")
    public String topBooks(Model model) {
        model.addAttribute("topBooks", reportService.topBorrowedBooks());
        model.addAttribute("activeTab", "top-books");
        return "reports/top-books";
    }

    @GetMapping("/members")
    public String memberStats(@PageableDefault Pageable pageable, Model model) {
        model.addAttribute("page", reportService.memberBorrowStats(pageable));
        model.addAttribute("activeTab", "members");
        return "reports/members";
    }
}
