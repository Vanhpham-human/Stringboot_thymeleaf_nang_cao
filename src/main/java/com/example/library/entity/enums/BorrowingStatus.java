package com.example.library.entity.enums;

import java.util.EnumSet;
import java.util.Set;

public enum BorrowingStatus {
    BORROWING("Đang mượn", "text-bg-primary-soft", "bi-hourglass-split"),
    PARTIALLY_RETURNED("Đã trả một phần", "text-bg-info-soft", "bi-pie-chart"),
    OVERDUE("Quá hạn", "text-bg-danger-soft", "bi-alarm"),
    RETURNED("Đã trả", "text-bg-success-soft", "bi-check2-circle"),
    FINE_PENDING("Chờ thanh toán phí", "text-bg-warning-soft", "bi-cash-coin"),
    FINE_PAID("Đã thanh toán phí", "text-bg-teal-soft", "bi-patch-check"),
    CANCELLED("Đã hủy", "text-bg-secondary-soft", "bi-x-circle");

    /** Các trạng thái phiếu vẫn còn sách chưa trả. */
    public static final Set<BorrowingStatus> OPEN_STATUSES = EnumSet.of(BORROWING, PARTIALLY_RETURNED, OVERDUE);

    private final String label;
    private final String badgeClass;
    private final String icon;

    BorrowingStatus(String label, String badgeClass, String icon) {
        this.label = label;
        this.badgeClass = badgeClass;
        this.icon = icon;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public String getIcon() {
        return icon;
    }

    public boolean isOpen() {
        return OPEN_STATUSES.contains(this);
    }
}
