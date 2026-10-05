package com.example.library.controller.web;

import com.example.library.dto.form.BookForm;
import com.example.library.entity.enums.BookStatus;
import com.example.library.exception.BusinessException;
import com.example.library.service.BookService;
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

import java.util.Arrays;

@Controller
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {

    private static final String FORM_VIEW = "books/form";

    private final BookService bookService;
    private final CategoryService categoryService;

    @ModelAttribute("bookStatuses")
    public BookStatus[] bookStatuses() {
        return Arrays.stream(BookStatus.values()).filter(s -> s != BookStatus.DELETED).toArray(BookStatus[]::new);
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) BookStatus status,
                       @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
                       Model model) {
        model.addAttribute("page", bookService.search(keyword, categoryId, status, pageable));
        model.addAttribute("categories", categoryService.findAllActive());
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("status", status);
        return "books/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("bookForm", BookForm.builder().totalQuantity(1).availableQuantity(1).build());
        return formView(model);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("bookForm") BookForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return formView(model);
        }
        try {
            bookService.create(form);
        } catch (BusinessException ex) {
            FormErrors.apply(bindingResult, ex);
            return formView(model);
        }
        redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sách \"" + form.getTitle().trim() + "\"");
        return "redirect:/books";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("book", bookService.getById(id));
        return "books/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("bookForm", bookService.getForm(id));
        model.addAttribute("book", bookService.getById(id));
        return formView(model);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("bookForm") BookForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        form.setId(id);
        if (bindingResult.hasErrors()) {
            model.addAttribute("book", bookService.getById(id));
            return formView(model);
        }
        try {
            bookService.update(id, form);
        } catch (BusinessException ex) {
            FormErrors.apply(bindingResult, ex);
            model.addAttribute("book", bookService.getById(id));
            return formView(model);
        }
        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật sách \"" + form.getTitle().trim() + "\"");
        return "redirect:/books";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        bookService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sách");
        return "redirect:/books";
    }

    private String formView(Model model) {
        model.addAttribute("categories", categoryService.findAllActive());
        return FORM_VIEW;
    }
}
