package com.example.library.controller.web;

import com.example.library.config.BorrowingProperties;
import com.example.library.dto.BorrowingDto;
import com.example.library.dto.BorrowingSearchCriteria;
import com.example.library.dto.form.BorrowingForm;
import com.example.library.dto.form.CancelBorrowingForm;
import com.example.library.dto.form.ReturnForm;
import com.example.library.entity.enums.BorrowingStatus;
import com.example.library.exception.BusinessException;
import com.example.library.service.BookService;
import com.example.library.service.BorrowingService;
import com.example.library.service.MemberService;
import com.example.library.util.Money;
import com.example.library.web.FormErrors;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;

@Controller
@RequestMapping("/borrowings")
@RequiredArgsConstructor
public class BorrowingController {

    private static final String FORM_VIEW = "borrowings/form";
    private static final String RETURN_VIEW = "borrowings/return";

    private final BorrowingService borrowingService;
    private final MemberService memberService;
    private final BookService bookService;
    private final BorrowingProperties borrowingProperties;

    @ModelAttribute("borrowingStatuses")
    public BorrowingStatus[] borrowingStatuses() {
        return BorrowingStatus.values();
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Long memberId,
                       @RequestParam(required = false) BorrowingStatus status,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                       @RequestParam(defaultValue = "false") boolean overdueOnly,
                       @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                       Model model) {
        BorrowingSearchCriteria criteria = new BorrowingSearchCriteria(keyword, memberId, status, fromDate, toDate, overdueOnly);
        model.addAttribute("page", borrowingService.search(criteria, pageable));
        model.addAttribute("filter", criteria);
        if (memberId != null) {
            model.addAttribute("filterMember", memberService.getById(memberId));
        }
        return "borrowings/list";
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(required = false) Long memberId, Model model) {
        BorrowingForm form = borrowingService.newBorrowingForm();
        form.setMemberId(memberId);
        model.addAttribute("borrowingForm", form);
        return formView(model);
    }

    /** Tạo phiếu mượn: toàn bộ nghiệp vụ chạy trong transaction ở {@link BorrowingService#createBorrowing}. */
    @PostMapping
    public String create(@Valid @ModelAttribute("borrowingForm") BorrowingForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return formView(model);
        }
        Long id;
        try {
            id = borrowingService.createBorrowing(form);
        } catch (BusinessException ex) {
            FormErrors.apply(bindingResult, ex);
            return formView(model);
        }
        redirectAttributes.addFlashAttribute("successMessage", "Tạo phiếu mượn #" + id + " thành công");
        return "redirect:/borrowings/" + id;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("borrowing", borrowingService.getDetail(id));
        if (!model.containsAttribute("cancelForm")) {
            model.addAttribute("cancelForm", new CancelBorrowingForm());
        }
        return "borrowings/detail";
    }

    @GetMapping("/{id}/return")
    public String returnForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        BorrowingDto borrowing = borrowingService.getDetail(id);
        if (!borrowing.isOpen()) {
            redirectAttributes.addFlashAttribute("errorMessage", borrowing.isCancelled()
                    ? "Phiếu mượn #" + id + " đã bị hủy, không thể trả sách"
                    : "Phiếu mượn #" + id + " đã được trả đủ sách");
            return "redirect:/borrowings/" + id;
        }
        model.addAttribute("borrowing", borrowing);
        model.addAttribute("returnForm", borrowingService.newReturnForm(id));
        return RETURN_VIEW;
    }

    /** Trả từng phần: cập nhật số lượng đã trả, tồn kho và phí phạt trong cùng một transaction. */
    @PostMapping("/{id}/return")
    public String returnBooks(@PathVariable Long id,
                              @Valid @ModelAttribute("returnForm") ReturnForm form,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                BorrowingDto result = borrowingService.returnBooks(id, form);
                redirectAttributes.addFlashAttribute("successMessage", buildReturnMessage(result));
                return "redirect:/borrowings/" + id;
            } catch (BusinessException ex) {
                FormErrors.apply(bindingResult, ex);
            }
        }
        model.addAttribute("borrowing", borrowingService.getDetail(id));
        return RETURN_VIEW;
    }

    @PostMapping("/{id}/return-all")
    public String returnAll(@PathVariable Long id,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate returnDate,
                            RedirectAttributes redirectAttributes) {
        BorrowingDto result = borrowingService.returnAll(id, returnDate);
        redirectAttributes.addFlashAttribute("successMessage", buildReturnMessage(result));
        return "redirect:/borrowings/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id,
                         @Valid @ModelAttribute("cancelForm") CancelBorrowingForm form,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/borrowings/" + id;
        }
        borrowingService.cancel(id, form.getReason());
        redirectAttributes.addFlashAttribute("successMessage", "Đã hủy phiếu mượn #" + id + " và hoàn lại tồn kho");
        return "redirect:/borrowings/" + id;
    }

    private String buildReturnMessage(BorrowingDto result) {
        StringBuilder message = new StringBuilder();
        if (result.isOpen()) {
            message.append("Đã ghi nhận trả sách. Phiếu còn ").append(result.getRemainingQuantity()).append(" cuốn chưa trả.");
        } else {
            message.append("Đã trả đủ sách, phiếu chuyển sang trạng thái \"").append(result.getStatus().getLabel()).append("\".");
        }
        if (result.hasUnpaidFine()) {
            message.append(" Phí phạt còn phải thu: ").append(Money.format(result.getUnpaidFineAmount())).append(" đ.");
        }
        return message.toString();
    }

    private String formView(Model model) {
        model.addAttribute("members", memberService.findActiveMembers());
        model.addAttribute("books", bookService.findBorrowable());
        model.addAttribute("defaultLoanDays", borrowingProperties.defaultLoanDays());
        model.addAttribute("maxBooksPerMember", borrowingProperties.maxBooksPerMember());
        return FORM_VIEW;
    }
}
