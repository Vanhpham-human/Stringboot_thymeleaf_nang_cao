-- =====================================================================
-- V1: Khởi tạo schema cho hệ thống quản lý mượn sách
-- =====================================================================

CREATE TABLE categories
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    active      BIT(1)       NOT NULL DEFAULT b'1',
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT uk_categories_name UNIQUE (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE books
(
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    isbn               VARCHAR(13)  NOT NULL,
    title              VARCHAR(255) NOT NULL,
    author             VARCHAR(150) NOT NULL,
    category_id        BIGINT       NOT NULL,
    total_quantity     INT          NOT NULL,
    available_quantity INT          NOT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    version            BIGINT       NOT NULL DEFAULT 0,
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    CONSTRAINT pk_books PRIMARY KEY (id),
    CONSTRAINT uk_books_isbn UNIQUE (isbn),
    CONSTRAINT fk_books_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT ck_books_total_quantity CHECK (total_quantity >= 0),
    CONSTRAINT ck_books_available_range CHECK (available_quantity >= 0 AND available_quantity <= total_quantity),
    CONSTRAINT ck_books_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_books_title ON books (title);
CREATE INDEX idx_books_author ON books (author);
CREATE INDEX idx_books_status ON books (status);

CREATE TABLE members
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    full_name  VARCHAR(150) NOT NULL,
    email      VARCHAR(150) NOT NULL,
    phone      VARCHAR(20)  NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    CONSTRAINT pk_members PRIMARY KEY (id),
    CONSTRAINT uk_members_email UNIQUE (email),
    CONSTRAINT ck_members_status CHECK (status IN ('ACTIVE', 'BLOCKED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_members_status ON members (status);

CREATE TABLE borrowings
(
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    member_id     BIGINT        NOT NULL,
    borrow_date   DATE          NOT NULL,
    due_date      DATE          NOT NULL,
    returned_date DATE          NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'BORROWING',
    total_fine    DECIMAL(12, 2) NOT NULL DEFAULT 0,
    note          VARCHAR(500)  NULL,
    created_at    DATETIME(6)   NOT NULL,
    updated_at    DATETIME(6)   NOT NULL,
    CONSTRAINT pk_borrowings PRIMARY KEY (id),
    CONSTRAINT fk_borrowings_member FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT ck_borrowings_due_date CHECK (due_date > borrow_date),
    CONSTRAINT ck_borrowings_total_fine CHECK (total_fine >= 0),
    CONSTRAINT ck_borrowings_status CHECK (status IN ('BORROWING', 'OVERDUE', 'RETURNED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_borrowings_status_due_date ON borrowings (status, due_date);
CREATE INDEX idx_borrowings_member ON borrowings (member_id);

CREATE TABLE borrowing_details
(
    id                 BIGINT         NOT NULL AUTO_INCREMENT,
    borrowing_id       BIGINT         NOT NULL,
    book_id            BIGINT         NOT NULL,
    quantity           INT            NOT NULL,
    returned_quantity  INT            NOT NULL DEFAULT 0,
    fine_amount        DECIMAL(12, 2) NOT NULL DEFAULT 0,
    last_returned_date DATE           NULL,
    created_at         DATETIME(6)    NOT NULL,
    updated_at         DATETIME(6)    NOT NULL,
    CONSTRAINT pk_borrowing_details PRIMARY KEY (id),
    CONSTRAINT fk_details_borrowing FOREIGN KEY (borrowing_id) REFERENCES borrowings (id) ON DELETE CASCADE,
    CONSTRAINT fk_details_book FOREIGN KEY (book_id) REFERENCES books (id),
    CONSTRAINT uk_details_borrowing_book UNIQUE (borrowing_id, book_id),
    CONSTRAINT ck_details_quantity CHECK (quantity > 0),
    CONSTRAINT ck_details_returned_range CHECK (returned_quantity >= 0 AND returned_quantity <= quantity),
    CONSTRAINT ck_details_fine CHECK (fine_amount >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_details_book ON borrowing_details (book_id);
