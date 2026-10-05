-- =====================================================================
-- V4: Dữ liệu mẫu cho quản lý phí phạt
--   * 2 chính sách phí (1 cũ đã ngừng, 1 đang áp dụng có mức trần)
--   * Phiếu #2: đã thanh toán đủ phí  -> FINE_PAID
--   * Phiếu #7: thanh toán một phần + được miễn giảm, còn nợ 30.000 -> FINE_PENDING
--     (độc giả #8 sẽ bị chặn tạo phiếu mới cho tới khi trả hết nợ)
--   * Phiếu #11: đã trả một phần, còn hạn -> PARTIALLY_RETURNED
--   * Phiếu #12: phiếu đã hủy (chưa trả sách, chưa phát sinh phí) -> CANCELLED
-- =====================================================================

INSERT INTO fine_policies (id, name, daily_fine_amount, max_fine_amount, grace_days, active, description, created_at, updated_at)
VALUES (1, 'Chính sách 2025', 3000, NULL, 0, b'0', 'Mức phí cũ, không giới hạn trần',
        NOW(6) - INTERVAL 300 DAY, NOW(6) - INTERVAL 120 DAY),
       (2, 'Chính sách 2026', 5000, 100000, 0, b'1',
        '5.000 đ / ngày / cuốn, phí mỗi dòng sách tối đa 100.000 đ',
        NOW(6) - INTERVAL 120 DAY, NOW(6) - INTERVAL 120 DAY);

-- Phiếu #2: trả muộn 6 ngày (30.000 đ), đã thanh toán đủ
INSERT INTO fine_payments (id, borrowing_id, amount, payment_date, method, note, created_at, updated_at)
VALUES (1, 2, 30000, CURDATE() - INTERVAL 9 DAY, 'CASH', 'Thanh toán tại quầy',
        NOW(6) - INTERVAL 9 DAY, NOW(6) - INTERVAL 9 DAY);

UPDATE borrowings
SET paid_fine_amount = 30000, unpaid_fine_amount = 0, status = 'FINE_PAID'
WHERE id = 2;

-- Phiếu #7: trả muộn 6 ngày x 2 cuốn (60.000 đ), đã trả 20.000 đ, được miễn 10.000 đ, còn nợ 30.000 đ
UPDATE borrowings
SET member_id = 8, paid_fine_amount = 20000, waived_fine_amount = 10000, unpaid_fine_amount = 30000,
    status = 'FINE_PENDING', note = 'Còn nợ phí, độc giả bị chặn mượn mới'
WHERE id = 7;

INSERT INTO fine_payments (id, borrowing_id, amount, payment_date, method, note, created_at, updated_at)
VALUES (2, 7, 20000, CURDATE() - INTERVAL 39 DAY, 'BANK_TRANSFER', 'Chuyển khoản lần 1',
        NOW(6) - INTERVAL 39 DAY, NOW(6) - INTERVAL 39 DAY);

INSERT INTO fine_waivers (id, borrowing_id, amount, reason, approved_by, approved_date, created_at, updated_at)
VALUES (1, 7, 10000, 'Sách bị trả muộn do độc giả nằm viện, có giấy xác nhận', 'Thủ thư Nguyễn Thị Mai',
        CURDATE() - INTERVAL 38 DAY, NOW(6) - INTERVAL 38 DAY, NOW(6) - INTERVAL 38 DAY);

-- Phiếu #11: mượn 2 cuốn Truyện Kiều, đã trả 1 cuốn đúng hạn
INSERT INTO borrowings (id, member_id, borrow_date, due_date, returned_date, status, total_fine_amount,
                        paid_fine_amount, waived_fine_amount, unpaid_fine_amount, note, created_at, updated_at)
VALUES (11, 2, CURDATE() - INTERVAL 6 DAY, CURDATE() + INTERVAL 8 DAY, NULL, 'PARTIALLY_RETURNED', 0, 0, 0, 0,
        'Đã trả trước 1 cuốn', NOW(6) - INTERVAL 6 DAY, NOW(6) - INTERVAL 1 DAY);

INSERT INTO borrowing_details (id, borrowing_id, book_id, quantity, returned_quantity, fine_amount,
                               last_returned_date, late_days, created_at, updated_at)
VALUES (18, 11, 7, 2, 1, 0, CURDATE() - INTERVAL 1 DAY, 0, NOW(6) - INTERVAL 6 DAY, NOW(6) - INTERVAL 1 DAY);

UPDATE books SET available_quantity = available_quantity - 1 WHERE id = 7;

-- Phiếu #12: đã hủy, tồn kho đã được hoàn lại
INSERT INTO borrowings (id, member_id, borrow_date, due_date, returned_date, cancelled_date, cancel_reason, status,
                        total_fine_amount, paid_fine_amount, waived_fine_amount, unpaid_fine_amount, note,
                        created_at, updated_at)
VALUES (12, 5, CURDATE() - INTERVAL 3 DAY, CURDATE() + INTERVAL 11 DAY, NULL, CURDATE() - INTERVAL 3 DAY,
        'Độc giả đổi ý, chưa nhận sách', 'CANCELLED', 0, 0, 0, 0, NULL, NOW(6) - INTERVAL 3 DAY, NOW(6) - INTERVAL 3 DAY);

INSERT INTO borrowing_details (id, borrowing_id, book_id, quantity, returned_quantity, fine_amount,
                               last_returned_date, late_days, created_at, updated_at)
VALUES (19, 12, 13, 1, 0, 0, NULL, 0, NOW(6) - INTERVAL 3 DAY, NOW(6) - INTERVAL 3 DAY);
