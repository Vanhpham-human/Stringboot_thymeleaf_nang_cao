package com.example.library.controller.web;

import com.example.library.dto.BorrowingSearchCriteria;
import com.example.library.dto.form.MemberForm;
import com.example.library.entity.enums.MemberStatus;
import com.example.library.exception.BusinessException;
import com.example.library.service.BorrowingService;
import com.example.library.service.MemberService;
import com.example.library.web.FormErrors;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private static final String FORM_VIEW = "members/form";

    private final MemberService memberService;
    private final BorrowingService borrowingService;

    @ModelAttribute("memberStatuses")
    public MemberStatus[] memberStatuses() {
        return MemberStatus.values();
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) MemberStatus status,
                       @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
                       Model model) {
        model.addAttribute("page", memberService.search(keyword, status, pageable));
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        return "members/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                         Model model) {
        model.addAttribute("member", memberService.getById(id));
        model.addAttribute("summary", borrowingService.memberSummary(id));
        model.addAttribute("page", borrowingService.search(BorrowingSearchCriteria.byMember(id), pageable));
        return "members/detail";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("memberForm", new MemberForm());
        return FORM_VIEW;
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("memberForm") MemberForm form,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            memberService.create(form);
        } catch (BusinessException ex) {
            FormErrors.apply(bindingResult, ex);
            return FORM_VIEW;
        }
        redirectAttributes.addFlashAttribute("successMessage", "Đã thêm thành viên \"" + form.getFullName().trim() + "\"");
        return "redirect:/members";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("memberForm", memberService.getForm(id));
        return FORM_VIEW;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("memberForm") MemberForm form,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        form.setId(id);
        if (bindingResult.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            memberService.update(id, form);
        } catch (BusinessException ex) {
            FormErrors.apply(bindingResult, ex);
            return FORM_VIEW;
        }
        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật thành viên \"" + form.getFullName().trim() + "\"");
        return "redirect:/members";
    }

    @PostMapping("/{id}/block")
    public String block(@PathVariable Long id,
                        @RequestParam(defaultValue = "/members") String redirect,
                        RedirectAttributes redirectAttributes) {
        memberService.block(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã khóa thành viên. Thành viên này không thể tạo phiếu mượn mới.");
        return "redirect:" + safeRedirect(redirect);
    }

    @PostMapping("/{id}/unblock")
    public String unblock(@PathVariable Long id,
                          @RequestParam(defaultValue = "/members") String redirect,
                          RedirectAttributes redirectAttributes) {
        memberService.unblock(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã mở khóa thành viên");
        return "redirect:" + safeRedirect(redirect);
    }

    private String safeRedirect(String redirect) {
        return redirect.startsWith("/members") ? redirect : "/members";
    }
}
