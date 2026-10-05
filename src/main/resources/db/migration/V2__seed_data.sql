-- =====================================================================
-- V2: Dữ liệu khởi tạo
--   * 7 thể loại (1 thể loại đã xóa mềm)
--   * 23 đầu sách thuộc nhiều thể loại
--   * 10 thành viên (8 ACTIVE, 2 BLOCKED)
--   * 10 phiếu mượn: có phiếu đang mượn, quá hạn, đã trả đúng hạn và trả muộn có tiền phạt
-- Ngày tháng được tính tương đối theo CURDATE() để dữ liệu quá hạn luôn có ý nghĩa khi chạy.
-- available_quantity = total_quantity - số cuốn đang được mượn chưa trả.
-- =====================================================================

INSERT INTO categories (id, name, description, active, created_at, updated_at)
VALUES (1, 'Công nghệ thông tin', 'Lập trình, kiến trúc phần mềm, cơ sở dữ liệu', b'1', NOW(6) - INTERVAL 90 DAY, NOW(6) - INTERVAL 90 DAY),
       (2, 'Văn học Việt Nam', 'Tác phẩm văn học kinh điển và đương đại Việt Nam', b'1', NOW(6) - INTERVAL 89 DAY, NOW(6) - INTERVAL 89 DAY),
       (3, 'Kinh tế - Kinh doanh', 'Quản trị, tài chính, khởi nghiệp', b'1', NOW(6) - INTERVAL 88 DAY, NOW(6) - INTERVAL 88 DAY),
       (4, 'Kỹ năng sống', 'Phát triển bản thân, giao tiếp', b'1', NOW(6) - INTERVAL 87 DAY, NOW(6) - INTERVAL 87 DAY),
       (5, 'Khoa học', 'Khoa học tự nhiên, vũ trụ, lịch sử loài người', b'1', NOW(6) - INTERVAL 86 DAY, NOW(6) - INTERVAL 86 DAY),
       (6, 'Thiếu nhi', 'Truyện tranh, truyện thiếu nhi', b'1', NOW(6) - INTERVAL 85 DAY, NOW(6) - INTERVAL 85 DAY),
       (7, 'Lịch sử', 'Sách lịch sử (đã ngừng sử dụng)', b'0', NOW(6) - INTERVAL 84 DAY, NOW(6) - INTERVAL 10 DAY);

INSERT INTO books (id, isbn, title, author, category_id, total_quantity, available_quantity, status, version, created_at, updated_at)
VALUES (1, '9780132350884', 'Clean Code', 'Robert C. Martin', 1, 5, 3, 'ACTIVE', 0, NOW(6) - INTERVAL 80 DAY, NOW(6) - INTERVAL 80 DAY),
       (2, '9780134685991', 'Effective Java', 'Joshua Bloch', 1, 4, 4, 'ACTIVE', 0, NOW(6) - INTERVAL 79 DAY, NOW(6) - INTERVAL 79 DAY),
       (3, '9780201633610', 'Design Patterns', 'Erich Gamma', 1, 3, 2, 'ACTIVE', 0, NOW(6) - INTERVAL 78 DAY, NOW(6) - INTERVAL 78 DAY),
       (4, '9781617294945', 'Spring in Action', 'Craig Walls', 1, 4, 2, 'ACTIVE', 0, NOW(6) - INTERVAL 77 DAY, NOW(6) - INTERVAL 77 DAY),
       (5, '9780134757599', 'Refactoring', 'Martin Fowler', 1, 3, 3, 'ACTIVE', 0, NOW(6) - INTERVAL 76 DAY, NOW(6) - INTERVAL 76 DAY),
       (6, '9786042088176', 'Dế Mèn phiêu lưu ký', 'Tô Hoài', 6, 6, 5, 'ACTIVE', 0, NOW(6) - INTERVAL 75 DAY, NOW(6) - INTERVAL 75 DAY),
       (7, '9786049691379', 'Truyện Kiều', 'Nguyễn Du', 2, 4, 4, 'ACTIVE', 0, NOW(6) - INTERVAL 74 DAY, NOW(6) - INTERVAL 74 DAY),
       (8, '9786049632013', 'Số đỏ', 'Vũ Trọng Phụng', 2, 5, 4, 'ACTIVE', 0, NOW(6) - INTERVAL 73 DAY, NOW(6) - INTERVAL 73 DAY),
       (9, '9786049632020', 'Tắt đèn', 'Ngô Tất Tố', 2, 3, 3, 'ACTIVE', 0, NOW(6) - INTERVAL 72 DAY, NOW(6) - INTERVAL 72 DAY),
       (10, '9786041091054', 'Mắt biếc', 'Nguyễn Nhật Ánh', 2, 6, 5, 'ACTIVE', 0, NOW(6) - INTERVAL 71 DAY, NOW(6) - INTERVAL 71 DAY),
       (11, '9786041112360', 'Cho tôi xin một vé đi tuổi thơ', 'Nguyễn Nhật Ánh', 2, 5, 5, 'ACTIVE', 0, NOW(6) - INTERVAL 70 DAY, NOW(6) - INTERVAL 70 DAY),
       (12, '9786045898734', 'Đắc nhân tâm', 'Dale Carnegie', 4, 8, 7, 'ACTIVE', 0, NOW(6) - INTERVAL 69 DAY, NOW(6) - INTERVAL 69 DAY),
       (13, '9786045651230', 'Tuổi trẻ đáng giá bao nhiêu', 'Rosie Nguyễn', 4, 4, 4, 'ACTIVE', 0, NOW(6) - INTERVAL 68 DAY, NOW(6) - INTERVAL 68 DAY),
       (14, '9786045623459', 'Đọc vị bất kỳ ai', 'David J. Lieberman', 4, 3, 3, 'ACTIVE', 0, NOW(6) - INTERVAL 67 DAY, NOW(6) - INTERVAL 67 DAY),
       (15, '9786045673218', 'Cha giàu cha nghèo', 'Robert Kiyosaki', 3, 5, 4, 'ACTIVE', 0, NOW(6) - INTERVAL 66 DAY, NOW(6) - INTERVAL 66 DAY),
       (16, '9786045812341', 'Từ tốt đến vĩ đại', 'Jim Collins', 3, 3, 3, 'ACTIVE', 0, NOW(6) - INTERVAL 65 DAY, NOW(6) - INTERVAL 65 DAY),
       (17, '9786045834565', 'Khởi nghiệp tinh gọn', 'Eric Ries', 3, 2, 2, 'ACTIVE', 0, NOW(6) - INTERVAL 64 DAY, NOW(6) - INTERVAL 64 DAY),
       (18, '9786045845677', 'Lược sử thời gian', 'Stephen Hawking', 5, 4, 4, 'ACTIVE', 0, NOW(6) - INTERVAL 63 DAY, NOW(6) - INTERVAL 63 DAY),
       (19, '9786045856789', 'Vũ trụ', 'Carl Sagan', 5, 2, 2, 'ACTIVE', 0, NOW(6) - INTERVAL 62 DAY, NOW(6) - INTERVAL 62 DAY),
       (20, '9786045867890', 'Sapiens: Lược sử loài người', 'Yuval Noah Harari', 5, 5, 4, 'ACTIVE', 0, NOW(6) - INTERVAL 61 DAY, NOW(6) - INTERVAL 61 DAY),
       (21, '9786042112345', 'Doraemon - Tập 1', 'Fujiko F. Fujio', 6, 10, 10, 'ACTIVE', 0, NOW(6) - INTERVAL 60 DAY, NOW(6) - INTERVAL 60 DAY),
       (22, '9786042123456', 'Hoàng tử bé', 'Antoine de Saint-Exupéry', 6, 4, 4, 'ACTIVE', 0, NOW(6) - INTERVAL 59 DAY, NOW(6) - INTERVAL 59 DAY),
       (23, '9786042134567', 'Việt Nam sử lược', 'Trần Trọng Kim', 7, 3, 3, 'INACTIVE', 0, NOW(6) - INTERVAL 58 DAY, NOW(6) - INTERVAL 58 DAY);

INSERT INTO members (id, full_name, email, phone, status, created_at, updated_at)
VALUES (1, 'Nguyễn Văn An', 'an.nguyen@example.com', '0901234561', 'ACTIVE', NOW(6) - INTERVAL 100 DAY, NOW(6) - INTERVAL 100 DAY),
       (2, 'Trần Thị Bình', 'binh.tran@example.com', '0901234562', 'ACTIVE', NOW(6) - INTERVAL 98 DAY, NOW(6) - INTERVAL 98 DAY),
       (3, 'Lê Hoàng Cường', 'cuong.le@example.com', '0901234563', 'ACTIVE', NOW(6) - INTERVAL 96 DAY, NOW(6) - INTERVAL 96 DAY),
       (4, 'Phạm Minh Dũng', 'dung.pham@example.com', '0901234564', 'ACTIVE', NOW(6) - INTERVAL 94 DAY, NOW(6) - INTERVAL 94 DAY),
       (5, 'Hoàng Thị Lan', 'lan.hoang@example.com', '0901234565', 'ACTIVE', NOW(6) - INTERVAL 92 DAY, NOW(6) - INTERVAL 92 DAY),
       (6, 'Vũ Đức Huy', 'huy.vu@example.com', '0901234566', 'ACTIVE', NOW(6) - INTERVAL 90 DAY, NOW(6) - INTERVAL 90 DAY),
       (7, 'Đặng Thu Hà', 'ha.dang@example.com', '0901234567', 'ACTIVE', NOW(6) - INTERVAL 88 DAY, NOW(6) - INTERVAL 88 DAY),
       (8, 'Bùi Quang Khải', 'khai.bui@example.com', '0901234568', 'ACTIVE', NOW(6) - INTERVAL 86 DAY, NOW(6) - INTERVAL 86 DAY),
       (9, 'Ngô Thanh Tùng', 'tung.ngo@example.com', '0901234569', 'BLOCKED', NOW(6) - INTERVAL 84 DAY, NOW(6) - INTERVAL 20 DAY),
       (10, 'Đỗ Mai Phương', 'phuong.do@example.com', NULL, 'BLOCKED', NOW(6) - INTERVAL 82 DAY, NOW(6) - INTERVAL 15 DAY);

INSERT INTO borrowings (id, member_id, borrow_date, due_date, returned_date, status, total_fine, note, created_at, updated_at)
VALUES
    -- Đã trả đúng hạn
    (1, 1, CURDATE() - INTERVAL 40 DAY, CURDATE() - INTERVAL 26 DAY, CURDATE() - INTERVAL 28 DAY, 'RETURNED', 0, 'Trả đúng hạn', NOW(6) - INTERVAL 40 DAY, NOW(6) - INTERVAL 28 DAY),
    -- Đã trả muộn 6 ngày x 1 cuốn = 30.000
    (2, 2, CURDATE() - INTERVAL 30 DAY, CURDATE() - INTERVAL 16 DAY, CURDATE() - INTERVAL 10 DAY, 'RETURNED', 30000, 'Trả muộn 6 ngày', NOW(6) - INTERVAL 30 DAY, NOW(6) - INTERVAL 10 DAY),
    -- Quá hạn, đã trả một phần
    (3, 3, CURDATE() - INTERVAL 25 DAY, CURDATE() - INTERVAL 11 DAY, NULL, 'OVERDUE', 0, 'Đã trả 1 cuốn Số đỏ', NOW(6) - INTERVAL 25 DAY, NOW(6) - INTERVAL 12 DAY),
    -- Quá hạn, chưa trả cuốn nào
    (4, 4, CURDATE() - INTERVAL 20 DAY, CURDATE() - INTERVAL 6 DAY, NULL, 'OVERDUE', 0, NULL, NOW(6) - INTERVAL 20 DAY, NOW(6) - INTERVAL 20 DAY),
    -- Đang mượn, còn hạn
    (5, 5, CURDATE() - INTERVAL 5 DAY, CURDATE() + INTERVAL 9 DAY, NULL, 'BORROWING', 0, NULL, NOW(6) - INTERVAL 5 DAY, NOW(6) - INTERVAL 5 DAY),
    (6, 6, CURDATE() - INTERVAL 2 DAY, CURDATE() + INTERVAL 12 DAY, NULL, 'BORROWING', 0, 'Mượn 2 cuốn cho nhóm học', NOW(6) - INTERVAL 2 DAY, NOW(6) - INTERVAL 2 DAY),
    -- Đã trả muộn 6 ngày x 2 cuốn = 60.000
    (7, 1, CURDATE() - INTERVAL 60 DAY, CURDATE() - INTERVAL 46 DAY, CURDATE() - INTERVAL 40 DAY, 'RETURNED', 60000, NULL, NOW(6) - INTERVAL 60 DAY, NOW(6) - INTERVAL 40 DAY),
    -- Quá hạn lâu: thành viên đã bị khóa
    (8, 9, CURDATE() - INTERVAL 50 DAY, CURDATE() - INTERVAL 36 DAY, NULL, 'OVERDUE', 0, 'Thành viên bị khóa do quá hạn lâu', NOW(6) - INTERVAL 50 DAY, NOW(6) - INTERVAL 20 DAY),
    -- Quá hạn 2 ngày
    (9, 7, CURDATE() - INTERVAL 16 DAY, CURDATE() - INTERVAL 2 DAY, NULL, 'OVERDUE', 0, NULL, NOW(6) - INTERVAL 16 DAY, NOW(6) - INTERVAL 16 DAY),
    -- Đã trả đúng hạn
    (10, 8, CURDATE() - INTERVAL 35 DAY, CURDATE() - INTERVAL 21 DAY, CURDATE() - INTERVAL 21 DAY, 'RETURNED', 0, NULL, NOW(6) - INTERVAL 35 DAY, NOW(6) - INTERVAL 21 DAY);

INSERT INTO borrowing_details (id, borrowing_id, book_id, quantity, returned_quantity, fine_amount, last_returned_date, created_at, updated_at)
VALUES (1, 1, 1, 1, 1, 0, CURDATE() - INTERVAL 28 DAY, NOW(6) - INTERVAL 40 DAY, NOW(6) - INTERVAL 28 DAY),
       (2, 1, 5, 1, 1, 0, CURDATE() - INTERVAL 28 DAY, NOW(6) - INTERVAL 40 DAY, NOW(6) - INTERVAL 28 DAY),
       (3, 2, 2, 1, 1, 30000, CURDATE() - INTERVAL 10 DAY, NOW(6) - INTERVAL 30 DAY, NOW(6) - INTERVAL 10 DAY),
       (4, 3, 1, 1, 0, 0, NULL, NOW(6) - INTERVAL 25 DAY, NOW(6) - INTERVAL 25 DAY),
       (5, 3, 8, 2, 1, 0, CURDATE() - INTERVAL 12 DAY, NOW(6) - INTERVAL 25 DAY, NOW(6) - INTERVAL 12 DAY),
       (6, 4, 3, 1, 0, 0, NULL, NOW(6) - INTERVAL 20 DAY, NOW(6) - INTERVAL 20 DAY),
       (7, 4, 10, 1, 0, 0, NULL, NOW(6) - INTERVAL 20 DAY, NOW(6) - INTERVAL 20 DAY),
       (8, 5, 1, 1, 0, 0, NULL, NOW(6) - INTERVAL 5 DAY, NOW(6) - INTERVAL 5 DAY),
       (9, 5, 12, 1, 0, 0, NULL, NOW(6) - INTERVAL 5 DAY, NOW(6) - INTERVAL 5 DAY),
       (10, 5, 15, 1, 0, 0, NULL, NOW(6) - INTERVAL 5 DAY, NOW(6) - INTERVAL 5 DAY),
       (11, 6, 4, 2, 0, 0, NULL, NOW(6) - INTERVAL 2 DAY, NOW(6) - INTERVAL 2 DAY),
       (12, 7, 3, 1, 1, 30000, CURDATE() - INTERVAL 40 DAY, NOW(6) - INTERVAL 60 DAY, NOW(6) - INTERVAL 40 DAY),
       (13, 7, 1, 1, 1, 30000, CURDATE() - INTERVAL 40 DAY, NOW(6) - INTERVAL 60 DAY, NOW(6) - INTERVAL 40 DAY),
       (14, 8, 6, 1, 0, 0, NULL, NOW(6) - INTERVAL 50 DAY, NOW(6) - INTERVAL 50 DAY),
       (15, 9, 20, 1, 0, 0, NULL, NOW(6) - INTERVAL 16 DAY, NOW(6) - INTERVAL 16 DAY),
       (16, 10, 12, 1, 1, 0, CURDATE() - INTERVAL 21 DAY, NOW(6) - INTERVAL 35 DAY, NOW(6) - INTERVAL 21 DAY),
       (17, 10, 20, 1, 1, 0, CURDATE() - INTERVAL 21 DAY, NOW(6) - INTERVAL 35 DAY, NOW(6) - INTERVAL 21 DAY);
