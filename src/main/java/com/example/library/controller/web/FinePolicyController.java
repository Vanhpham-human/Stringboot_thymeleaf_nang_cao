package com.example.library.controller.web;

import com.example.library.dto.form.FinePolicyForm;
import com.example.library.exception.BusinessException;
import com.example.library.service.FinePolicyService;
import com.example.library.web.FormErrors;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Cấu hình phí phạt. Chính sách không sửa trực tiếp mà tạo phiên bản mới rồi kích hoạt,
 * để các khoản phí đã tính trước đó vẫn truy được theo chính sách cũ.
 */
@Controller
@RequestMapping("/fine-policies")
@RequiredArgsConstructor
public class FinePolicyController {

    private static final String VIEW = "fine-policies/list";

    private final FinePolicyService finePolicyService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("policyForm", finePolicyService.newForm());
        return view(model);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("policyForm") FinePolicyForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                finePolicyService.create(form);
                redirectAttributes.addFlashAttribute("successMessage", form.isActivate()
                        ? "Đã tạo và áp dụng chính sách \"" + form.getName().trim() + "\""
                        : "Đã lưu chính sách \"" + form.getName().trim() + "\" (chưa áp dụng)");
                return "redirect:/fine-policies";
            } catch (BusinessException ex) {
                FormErrors.apply(bindingResult, ex);
            }
        }
        return view(model);
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        finePolicyService.activate(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã chuyển sang áp dụng chính sách #" + id);
        return "redirect:/fine-policies";
    }

    private String view(Model model) {
        model.addAttribute("policies", finePolicyService.findAll());
        model.addAttribute("activePolicy", finePolicyService.getActivePolicyDto());
        return VIEW;
    }
}
