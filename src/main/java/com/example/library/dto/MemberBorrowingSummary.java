package com.example.library.dto;

import java.math.BigDecimal;

/** Tình trạng mượn của một độc giả: dùng để hiển thị và giải thích vì sao bị chặn mượn. */
public record MemberBorrowingSummary(boolean blocked,
                                     long unreturnedBooks,
                                     int maxBooks,
                                     boolean hasOverdue,
                                     BigDecimal unpaidFine) {

    public long remainingSlots() {
        return Math.max(0, maxBooks - unreturnedBooks);
    }

    public boolean hasUnpaidFine() {
        return unpaidFine != null && unpaidFine.signum() > 0;
    }

    public boolean canBorrow() {
        return !blocked && !hasOverdue && !hasUnpaidFine() && remainingSlots() > 0;
    }
}
