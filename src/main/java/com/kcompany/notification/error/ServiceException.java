package com.kcompany.notification.error;

public class ServiceException extends Exception {

    private final ErrorDetails errorDetails;
    private final String[] variables;

    public ServiceException(final ErrorDetails errorDetails, final String ... variables) {
        this.errorDetails = errorDetails;
        this.variables = variables;
    }

    public String getErrorCode() {
        return errorDetails.getErrorCode();
    }

    public String[] getVariables() {
        return variables;
    }
}
