package com.example.library.entity;

import com.example.library.entity.enums.BookStatus;
import com.example.library.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "books")
public class Book extends BaseEntity {

    @Column(nullable = false, unique = true, length = 13)
    private String isbn;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 150)
    private String author;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private BookStatus status = BookStatus.ACTIVE;

    /** Optimistic locking: tránh mượn vượt tồn kho khi có nhiều request đồng thời. */
    @Version
    @Column(nullable = false)
    private Long version;

    public int getBorrowedQuantity() {
        return totalQuantity - availableQuantity;
    }

    public boolean isBorrowable() {
        return status == BookStatus.ACTIVE;
    }

    public void decreaseAvailable(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException("Số lượng mượn phải lớn hơn 0");
        }
        if (availableQuantity < quantity) {
            throw new BusinessException("Sách \"%s\" chỉ còn %d cuốn, không đủ để mượn %d cuốn"
                    .formatted(title, availableQuantity, quantity));
        }
        availableQuantity -= quantity;
    }

    public void increaseAvailable(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException("Số lượng trả phải lớn hơn 0");
        }
        if (availableQuantity + quantity > totalQuantity) {
            throw new BusinessException("Số lượng đang có của sách \"%s\" không được vượt quá tổng số lượng (%d)"
                    .formatted(title, totalQuantity));
        }
        availableQuantity += quantity;
    }
}
