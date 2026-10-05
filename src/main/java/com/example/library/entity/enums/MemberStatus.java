package com.example.library.entity.enums;

public enum MemberStatus {
    ACTIVE("Hoạt động"),
    BLOCKED("Bị khóa");

    private final String label;

    MemberStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
