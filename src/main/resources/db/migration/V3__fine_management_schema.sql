-- =====================================================================
-- V3: Nâng cấp quản lý phí phạt
--   * borrowings: tách tổng phí / đã thanh toán / miễn giảm / còn phải thu,
--     thêm trạng thái PARTIALLY_RETURNED, FINE_PENDING, FINE_PAID, CANCELLED,
--     thêm thông tin hủy phiếu và cột version cho optimistic locking
--   * borrowing_details: lưu số ngày trễ của lần trả gần nhất
--   * fine_policies: cấu hình phí phạt, tại một thời điểm chỉ có một policy active
--   * fine_payments: lịch sử thanh toán phí (thanh toán nhiều lần)
--   * fine_waivers: lịch sử miễn giảm phí có lý do và người duyệt
-- =====================================================================

-- ---------- borrowings ----------
ALTER TABLE borrowings DROP CHECK ck_borrowings_status;
ALTER TABLE borrowings DROP CHECK ck_borrowings_total_fine;

ALTER TABLE borrowings
    CHANGE COLUMN total_fine total_fine_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN paid_fine_amount   DECIMAL(12, 2) NOT NULL DEFAULT 0 AFTER total_fine_amount,
    ADD COLUMN waived_fine_amount DECIMAL(12, 2) NOT NULL DEFAULT 0 AFTER paid_fine_amount,
    ADD COLUMN unpaid_fine_amount DECIMAL(12, 2) NOT NULL DEFAULT 0 AFTER waived_fine_amount,
    ADD COLUMN cancelled_date     DATE           NULL AFTER returned_date,
    ADD COLUMN cancel_reason      VARCHAR(255)   NULL AFTER cancelled_date,
    ADD COLUMN version            BIGINT         NOT NULL DEFAULT 0;

-- Dữ liệu cũ: toàn bộ phí đã phát sinh đều chưa thu
UPDATE borrowings SET unpaid_fine_amount = total_fine_amount;

-- Chuyển trạng thái dữ liệu cũ sang mô hình mới
UPDATE borrowings SET status = 'FINE_PENDING'
WHERE status = 'RETURNED' AND unpaid_fine_amount > 0;

UPDATE borrowings b SET b.status = 'PARTIALLY_RETURNED'
WHERE b.status = 'BORROWING'
  AND EXISTS (SELECT 1 FROM borrowing_details d WHERE d.borrowing_id = b.id AND d.returned_quantity > 0);

ALTER TABLE borrowings
    ADD CONSTRAINT ck_borrowings_status CHECK (status IN ('BORROWING', 'PARTIALLY_RETURNED', 'OVERDUE', 'RETURNED',
                                                          'FINE_PENDING', 'FINE_PAID', 'CANCELLED')),
    ADD CONSTRAINT ck_borrowings_fine_non_negative CHECK (total_fine_amount >= 0 AND paid_fine_amount >= 0
        AND waived_fine_amount >= 0 AND unpaid_fine_amount >= 0),
    ADD CONSTRAINT ck_borrowings_fine_balance CHECK (paid_fine_amount + waived_fine_amount + unpaid_fine_amount = total_fine_amount);

CREATE INDEX idx_borrowings_member_status ON borrowings (member_id, status);
CREATE INDEX idx_borrowings_borrow_date ON borrowings (borrow_date);
CREATE INDEX idx_borrowings_unpaid ON borrowings (unpaid_fine_amount);

-- ---------- borrowing_details ----------
ALTER TABLE borrowing_details
    ADD COLUMN late_days INT NOT NULL DEFAULT 0 AFTER last_returned_date,
    ADD CONSTRAINT ck_details_late_days CHECK (late_days >= 0);

UPDATE borrowing_details d JOIN borrowings b ON b.id = d.borrowing_id
SET d.late_days = GREATEST(0, DATEDIFF(d.last_returned_date, b.due_date))
WHERE d.last_returned_date IS NOT NULL;

-- ---------- fine_policies ----------
CREATE TABLE fine_policies
(
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    name              VARCHAR(100)   NOT NULL,
    daily_fine_amount DECIMAL(12, 2) NOT NULL,
    max_fine_amount   DECIMAL(12, 2) NULL,
    grace_days        INT            NOT NULL DEFAULT 0,
    active            BIT(1)         NOT NULL DEFAULT b'0',
    description       VARCHAR(500)   NULL,
    created_at        DATETIME(6)    NOT NULL,
    updated_at        DATETIME(6)    NOT NULL,
    -- NULL khi không active => UNIQUE chỉ chặn trường hợp có hai policy active
    active_marker     TINYINT AS (CASE WHEN active = 1 THEN 1 ELSE NULL END) STORED,
    CONSTRAINT pk_fine_policies PRIMARY KEY (id),
    CONSTRAINT uk_fine_policies_single_active UNIQUE (active_marker),
    CONSTRAINT ck_fine_policies_daily CHECK (daily_fine_amount > 0),
    CONSTRAINT ck_fine_policies_max CHECK (max_fine_amount IS NULL OR max_fine_amount >= daily_fine_amount),
    CONSTRAINT ck_fine_policies_grace CHECK (grace_days >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ---------- fine_payments ----------
CREATE TABLE fine_payments
(
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    borrowing_id BIGINT         NOT NULL,
    amount       DECIMAL(12, 2) NOT NULL,
    payment_date DATE           NOT NULL,
    method       VARCHAR(20)    NOT NULL,
    note         VARCHAR(500)   NULL,
    created_at   DATETIME(6)    NOT NULL,
    updated_at   DATETIME(6)    NOT NULL,
    CONSTRAINT pk_fine_payments PRIMARY KEY (id),
    CONSTRAINT fk_fine_payments_borrowing FOREIGN KEY (borrowing_id) REFERENCES borrowings (id),
    CONSTRAINT ck_fine_payments_amount CHECK (amount > 0),
    CONSTRAINT ck_fine_payments_method CHECK (method IN ('CASH', 'BANK_TRANSFER', 'E_WALLET'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_fine_payments_borrowing ON fine_payments (borrowing_id);
CREATE INDEX idx_fine_payments_date ON fine_payments (payment_date);

-- ---------- fine_waivers ----------
CREATE TABLE fine_waivers
(
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    borrowing_id  BIGINT         NOT NULL,
    amount        DECIMAL(12, 2) NOT NULL,
    reason        VARCHAR(500)   NOT NULL,
    approved_by   VARCHAR(100)   NOT NULL,
    approved_date DATE           NOT NULL,
    created_at    DATETIME(6)    NOT NULL,
    updated_at    DATETIME(6)    NOT NULL,
    CONSTRAINT pk_fine_waivers PRIMARY KEY (id),
    CONSTRAINT fk_fine_waivers_borrowing FOREIGN KEY (borrowing_id) REFERENCES borrowings (id),
    CONSTRAINT ck_fine_waivers_amount CHECK (amount > 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_fine_waivers_borrowing ON fine_waivers (borrowing_id);
