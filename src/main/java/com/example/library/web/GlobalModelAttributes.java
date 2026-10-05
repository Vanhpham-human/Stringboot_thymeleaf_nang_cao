package com.example.library.web;

import com.example.library.config.BorrowingProperties;
import com.example.library.dto.FinePolicyDto;
import com.example.library.service.FinePolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Các giá trị cấu hình dùng chung cho mọi màn hình Thymeleaf. */
@ControllerAdvice(basePackages = "com.example.library.controller.web")
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final BorrowingProperties borrowingProperties;
    private final FinePolicyService finePolicyService;

    /** Chính sách phí phạt đang áp dụng (hoặc mức mặc định trong application.yml nếu chưa có). */
    @ModelAttribute("finePolicy")
    public FinePolicyDto finePolicy() {
        return finePolicyService.getActivePolicyDto();
    }

    @ModelAttribute("loanDays")
    public int loanDays() {
        return borrowingProperties.defaultLoanDays();
    }

    @ModelAttribute("maxBooks")
    public int maxBooks() {
        return borrowingProperties.maxBooksPerMember();
    }
}
