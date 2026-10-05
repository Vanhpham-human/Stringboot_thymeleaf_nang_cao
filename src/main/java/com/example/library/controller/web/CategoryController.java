package com.example.library.controller.web;

import com.example.library.dto.form.CategoryForm;
import com.example.library.exception.BusinessException;
import com.example.library.service.CategoryService;
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
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private static final String FORM_VIEW = "categories/form";

    private final CategoryService categoryService;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Boolean active,
                       @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
                       Model model) {
        model.addAttribute("page", categoryService.search(keyword, active, pageable));
        model.addAttribute("keyword", keyword);
        model.addAttribute("active", active);
        return "categories/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("categoryForm", new CategoryForm());
        return FORM_VIEW;
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("categoryForm") CategoryForm form,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            categoryService.create(form);
        } catch (BusinessException ex) {
            FormErrors.apply(bindingResult, ex);
            return FORM_VIEW;
        }
        redirectAttributes.addFlashAttribute("successMessage", "Đã thêm thể loại \"" + form.getName().trim() + "\"");
        return "redirect:/categories";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("categoryForm", categoryService.getForm(id));
        return FORM_VIEW;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("categoryForm") CategoryForm form,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        form.setId(id);
        if (bindingResult.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            categoryService.update(id, form);
        } catch (BusinessException ex) {
            FormErrors.apply(bindingResult, ex);
            return FORM_VIEW;
        }
        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật thể loại \"" + form.getName().trim() + "\"");
        return "redirect:/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        categoryService.softDelete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã ngừng sử dụng thể loại (xóa mềm)");
        return "redirect:/categories";
    }

    @PostMapping("/{id}/restore")
    public String restore(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        categoryService.restore(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã khôi phục thể loại");
        return "redirect:/categories";
    }
}
