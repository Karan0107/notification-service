package com.kcompany.notification.error;

public enum ErrorDetails {

    INTERNAL_ERROR_OCCURED("10102001");

    private String errorCode;

    ErrorDetails(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
