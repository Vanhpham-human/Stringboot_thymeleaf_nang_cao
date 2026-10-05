package com.example.library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * @param defaultLoanDays     hạn trả mặc định (ngày) khi không nhập hạn trả
 * @param maxBooksPerMember   số cuốn tối đa một độc giả được giữ (chưa trả) cùng lúc
 * @param defaultFinePerDay   mức phí dự phòng khi chưa có chính sách phí nào được kích hoạt
 */
@ConfigurationProperties(prefix = "app.borrowing")
public record BorrowingProperties(int defaultLoanDays, int maxBooksPerMember, BigDecimal defaultFinePerDay) {

    public BorrowingProperties {
        if (defaultLoanDays <= 0) {
            defaultLoanDays = 14;
        }
        if (maxBooksPerMember <= 0) {
            maxBooksPerMember = 5;
        }
        if (defaultFinePerDay == null || defaultFinePerDay.signum() <= 0) {
            defaultFinePerDay = BigDecimal.valueOf(5000);
        }
    }
}
