package com.example.hrcore.entity.enums;

public enum AbsenceRequestStatus {
    PENDING,
    APPROVED,
    REJECTED;

    public static AbsenceRequestStatus fromString(String status) {
        if (status == null) {
            return PENDING;
        }
        try {
            return AbsenceRequestStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PENDING;
        }
    }
}
