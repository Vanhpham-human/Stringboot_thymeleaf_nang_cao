package com.example.library.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.net.URI;

/**
 * Xử lý lỗi tập trung cho các controller trả về view Thymeleaf.
 * <ul>
 *     <li>Lỗi nghiệp vụ: quay lại trang trước kèm thông báo flash dễ hiểu.</li>
 *     <li>Không tìm thấy dữ liệu: trang 404.</li>
 *     <li>Lỗi không mong muốn: trang 500, ghi log chi tiết.</li>
 * </ul>
 */
@Slf4j
@ControllerAdvice(basePackages = "com.example.library.controller.web")
public class GlobalExceptionHandler {

    static final String CONCURRENT_UPDATE_MESSAGE =
            "Dữ liệu vừa được người khác cập nhật (ví dụ số lượng sách thay đổi). Vui lòng tải lại trang và thử lại.";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView handleNotFound(ResourceNotFoundException ex, HttpServletResponse response) {
        return errorPage(HttpStatus.NOT_FOUND, ex.getMessage(), response);
    }

    @ExceptionHandler(BusinessException.class)
    public ModelAndView handleBusiness(BusinessException ex, HttpServletRequest request, HttpServletResponse response) {
        log.info("Business rule violated: {}", ex.getMessage());
        return redirectBackWithError(ex.getMessage(), request, response);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ModelAndView handleOptimisticLock(ObjectOptimisticLockingFailureException ex,
                                             HttpServletRequest request, HttpServletResponse response) {
        log.warn("Optimistic locking conflict: {}", ex.getMessage());
        return redirectBackWithError(CONCURRENT_UPDATE_MESSAGE, request, response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ModelAndView handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletResponse response) {
        String message = "Giá trị '" + ex.getValue() + "' không hợp lệ cho tham số '" + ex.getName() + "'";
        return errorPage(HttpStatus.BAD_REQUEST, message, response);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ModelAndView handleInvalidSort(PropertyReferenceException ex, HttpServletResponse response) {
        return errorPage(HttpStatus.BAD_REQUEST, "Không thể sắp xếp theo trường '" + ex.getPropertyName() + "'", response);
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpected(Exception ex, HttpServletResponse response) {
        log.error("Unexpected error", ex);
        return errorPage(HttpStatus.INTERNAL_SERVER_ERROR,
                "Hệ thống gặp sự cố không mong muốn. Vui lòng thử lại sau.", response);
    }

    private ModelAndView redirectBackWithError(String message, HttpServletRequest request, HttpServletResponse response) {
        String target = safeRefererPath(request);
        if (target == null) {
            return errorPage(HttpStatus.BAD_REQUEST, message, response);
        }
        RequestContextUtils.getOutputFlashMap(request).put("errorMessage", message);
        return new ModelAndView("redirect:" + target);
    }

    /** Chỉ lấy path + query của Referer để tránh open redirect sang domain khác. */
    private String safeRefererPath(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (!StringUtils.hasText(referer)) {
            return null;
        }
        try {
            URI uri = URI.create(referer);
            if (!StringUtils.hasText(uri.getRawPath())) {
                return null;
            }
            String contextPath = request.getContextPath();
            String path = uri.getRawPath();
            if (StringUtils.hasText(contextPath) && path.startsWith(contextPath)) {
                path = path.substring(contextPath.length());
            }
            return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private ModelAndView errorPage(HttpStatus status, String message, HttpServletResponse response) {
        response.setStatus(status.value());
        ModelAndView mav = new ModelAndView("error/error");
        mav.setStatus(status);
        mav.addObject("status", status.value());
        mav.addObject("error", status.getReasonPhrase());
        mav.addObject("message", message);
        return mav;
    }
}
