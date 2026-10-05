package com.example.library.dto.report;

import com.example.library.entity.enums.MemberStatus;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class MemberBorrowStat {

    private final Long memberId;
    private final String fullName;
    private final String email;
    private final MemberStatus status;
    private final long borrowingCount;
    private final long totalBooks;
    private final long unreturnedBooks;
    private final BigDecimal totalFine;

    public MemberBorrowStat(Long memberId, String fullName, String email, MemberStatus status,
                            Number borrowingCount, Number totalBooks, Number unreturnedBooks, Number totalFine) {
        this.memberId = memberId;
        this.fullName = fullName;
        this.email = email;
        this.status = status;
        this.borrowingCount = borrowingCount == null ? 0 : borrowingCount.longValue();
        this.totalBooks = totalBooks == null ? 0 : totalBooks.longValue();
        this.unreturnedBooks = unreturnedBooks == null ? 0 : unreturnedBooks.longValue();
        this.totalFine = totalFine == null ? BigDecimal.ZERO : new BigDecimal(totalFine.toString());
    }
}
