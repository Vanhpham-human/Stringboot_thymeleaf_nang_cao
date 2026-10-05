package com.example.library.entity.enums;

public enum BookStatus {
    ACTIVE("Đang lưu hành"),
    INACTIVE("Ngừng lưu hành"),
    DELETED("Đã xóa");

    private final String label;

    BookStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
