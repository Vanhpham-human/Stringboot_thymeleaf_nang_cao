package com.example.library.controller.web;

import com.example.library.dto.form.FinePaymentForm;
import com.example.library.dto.form.FineWaiverForm;
import com.example.library.entity.enums.PaymentMethod;
import com.example.library.exception.BusinessException;
import com.example.library.service.BorrowingService;
import com.example.library.service.FinePaymentService;
import com.example.library.service.FinePaymentService.FineSettlementResult;
import com.example.library.util.Money;
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

/** Thanh toán và miễn giảm phí phạt của một phiếu mượn. */
@Controller
@RequestMapping("/borrowings/{id}")
@RequiredArgsConstructor
public class FineController {

    private static final String PAYMENT_VIEW = "borrowings/payment-form";
    private static final String WAIVER_VIEW = "borrowings/waiver-form";

    private final FinePaymentService finePaymentService;
    private final BorrowingService borrowingService;

    @ModelAttribute("paymentMethods")
    public PaymentMethod[] paymentMethods() {
        return PaymentMethod.values();
    }

    @GetMapping("/payments/new")
    public String paymentForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("paymentForm", finePaymentService.newPaymentForm(id));
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/borrowings/" + id;
        }
        model.addAttribute("borrowing", borrowingService.getDetail(id));
        return PAYMENT_VIEW;
    }

    @PostMapping("/payments")
    public String recordPayment(@PathVariable Long id,
                                @Valid @ModelAttribute("paymentForm") FinePaymentForm form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                FineSettlementResult result = finePaymentService.recordPayment(id, form);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã ghi nhận thanh toán " + Money.format(result.amount()) + " đ. " + remainingText(result));
                return "redirect:/borrowings/" + id;
            } catch (BusinessException ex) {
                FormErrors.apply(bindingResult, ex);
            }
        }
        model.addAttribute("borrowing", borrowingService.getDetail(id));
        return PAYMENT_VIEW;
    }

    @GetMapping("/waivers/new")
    public String waiverForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("waiverForm", finePaymentService.newWaiverForm(id));
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/borrowings/" + id;
        }
        model.addAttribute("borrowing", borrowingService.getDetail(id));
        return WAIVER_VIEW;
    }

    @PostMapping("/waivers")
    public String recordWaiver(@PathVariable Long id,
                               @Valid @ModelAttribute("waiverForm") FineWaiverForm form,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                FineSettlementResult result = finePaymentService.recordWaiver(id, form);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã miễn giảm " + Money.format(result.amount()) + " đ. " + remainingText(result));
                return "redirect:/borrowings/" + id;
            } catch (BusinessException ex) {
                FormErrors.apply(bindingResult, ex);
            }
        }
        model.addAttribute("borrowing", borrowingService.getDetail(id));
        return WAIVER_VIEW;
    }

    private String remainingText(FineSettlementResult result) {
        return result.fullySettled()
                ? "Phiếu đã thanh toán đủ phí phạt."
                : "Còn phải thu " + Money.format(result.remainingUnpaid()) + " đ.";
    }
}
