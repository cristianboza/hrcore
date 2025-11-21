package com.example.hrcore.entity.enums;

public enum AbsenceRequestType {
    VACATION,
    SICK,
    OTHER;

    public static AbsenceRequestType fromString(String type) {
        if (type == null) {
            return OTHER;
        }
        try {
            return AbsenceRequestType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }
}
