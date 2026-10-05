package com.example.library.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Sinh URL phân trang / sắp xếp giữ nguyên các tham số lọc hiện tại.
 * Dùng trong Thymeleaf qua cú pháp {@code ${@urls.page(2)}}.
 */
@Component("urls")
public class UrlHelper {

    public String page(int page) {
        return current().replaceQueryParam("page", page).build().toUriString();
    }

    public String size(int size) {
        return current().replaceQueryParam("size", size).replaceQueryParam("page", 0).build().toUriString();
    }

    /** Click lần đầu sắp xếp tăng dần, click lần nữa đổi chiều. */
    public String sort(String property) {
        String direction = property.equals(currentSortProperty()) && "asc".equals(currentSortDirection()) ? "desc" : "asc";
        return current()
                .replaceQueryParam("sort", property + "," + direction)
                .replaceQueryParam("page", 0)
                .build().toUriString();
    }

    public String sortIcon(String property) {
        if (!property.equals(currentSortProperty())) {
            return "bi-arrow-down-up text-body-tertiary";
        }
        return "asc".equals(currentSortDirection()) ? "bi-sort-up" : "bi-sort-down";
    }

    /** Các số trang hiển thị quanh trang hiện tại (tối đa 5 trang). */
    public List<Integer> pageNumbers(Page<?> page) {
        int total = page.getTotalPages();
        if (total <= 0) {
            return List.of();
        }
        int start = Math.max(0, Math.min(page.getNumber() - 2, total - 5));
        int end = Math.min(total - 1, start + 4);
        return IntStream.rangeClosed(start, end).boxed().toList();
    }

    /** Dùng để tô sáng menu đang chọn; "/" chỉ khớp chính xác trang chủ. */
    public boolean active(String prefix) {
        String path = request().getRequestURI();
        if ("/".equals(prefix)) {
            return "/".equals(path);
        }
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    private String currentSortProperty() {
        String sort = request().getParameter("sort");
        if (sort == null || sort.isBlank()) {
            return null;
        }
        return sort.split(",")[0];
    }

    private String currentSortDirection() {
        String sort = request().getParameter("sort");
        if (sort == null || !sort.contains(",")) {
            return "asc";
        }
        return sort.substring(sort.indexOf(',') + 1).trim().toLowerCase();
    }

    private UriComponentsBuilder current() {
        HttpServletRequest request = request();
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(request.getRequestURI());
        if (request.getQueryString() != null) {
            builder.query(request.getQueryString());
        }
        return builder;
    }

    private HttpServletRequest request() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    }
}
