package com.example.library.dto;

import com.example.library.entity.enums.BorrowingStatus;

import java.time.LocalDate;

/** Bộ lọc màn hình danh sách phiếu mượn: trạng thái, độc giả, khoảng ngày mượn, chỉ phiếu quá hạn. */
public record BorrowingSearchCriteria(String keyword,
                                      Long memberId,
                                      BorrowingStatus status,
                                      LocalDate fromDate,
                                      LocalDate toDate,
                                      boolean overdueOnly) {

    public static BorrowingSearchCriteria empty() {
        return new BorrowingSearchCriteria(null, null, null, null, null, false);
    }

    public static BorrowingSearchCriteria byMember(Long memberId) {
        return new BorrowingSearchCriteria(null, memberId, null, null, null, false);
    }

    public BorrowingSearchCriteria {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            LocalDate tmp = fromDate;
            fromDate = toDate;
            toDate = tmp;
        }
    }
}
