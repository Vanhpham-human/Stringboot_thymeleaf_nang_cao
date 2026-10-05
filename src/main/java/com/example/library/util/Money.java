package com.example.library.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/** Định dạng tiền VND kiểu 1.234.567 dùng trong thông báo lỗi / thông báo thành công. */
public final class Money {

    private Money() {
    }

    public static String format(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0", symbols).format(amount == null ? BigDecimal.ZERO : amount);
    }
}
