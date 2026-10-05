package com.example.library.dto.report;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class DashboardStats {

    private final long totalTitles;
    private final long totalCopies;
    private final long availableCopies;
    private final long activeMembers;
    private final long blockedMembers;
    private final long openBorrowings;
    private final long overdueBorrowings;
    private final long finePendingBorrowings;
    private final BigDecimal totalFine;
    private final BigDecimal paidFine;
    private final BigDecimal waivedFine;
    private final BigDecimal unpaidFine;

    public long getBorrowedCopies() {
        return totalCopies - availableCopies;
    }
}
