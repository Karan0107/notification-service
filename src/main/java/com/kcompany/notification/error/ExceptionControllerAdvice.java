package com.kcompany.notification.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@ControllerAdvice(annotations = RestController.class)
public class ExceptionControllerAdvice extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionControllerAdvice.class);

    @Autowired
    @Qualifier("errorCodes")
    private MessageSource messageSource;

    private Locale currentLocale = LocaleContextHolder.getLocale();

    private static final String UNEXPECTED_ERROR = "Unexpected Error ";
    private static final String TRACE_ID = "traceId";

    @ExceptionHandler({ServiceException.class})
    public ResponseEntity<ErrorResponse> handleServiceException(final Exception ex, WebRequest request) {
        ErrorResponse error;
        if(ex instanceof ServiceException) {
            ServiceException serviceException = (ServiceException)ex;
            final String errorMessage = String.format(currentLocale, getErrorMessage(serviceException.getErrorCode()), serviceException.getVariables());
            logger.error("API ERROR [{}] : [{}]", serviceException.getErrorCode(), errorMessage, serviceException);
            error = new ErrorResponse(serviceException.getErrorCode(), errorMessage);
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } else {
            logger.error(UNEXPECTED_ERROR, ex);
            error = getErrorResponse(ex);
            MDC.remove(TRACE_ID);
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ExceptionHandler({IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(Exception ex, WebRequest request) {
        logger.error(UNEXPECTED_ERROR, ex);
        ErrorResponse error = getErrorResponse(ex);
        MDC.remove(TRACE_ID);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    private ErrorResponse getErrorResponse(Exception ex) {
        ErrorResponse error;
        if(ex.getMessage() != null && ex.getMessage().contains("@")) {
            String[] split = ex.getMessage().split("@");
            if(split.length > 1) {
                final String errorMessage = String.format(currentLocale, getErrorMessage(split[0]), getStringFormattedArgs(Arrays.copyOfRange(split, 1, split.length)));
                error = new ErrorResponse(split[0], errorMessage);
            } else {
                final String errorMessage = String.format(currentLocale, getErrorMessage(split[0]), "");
                error = new ErrorResponse(split[0], errorMessage);
            }
        } else {
            error = new ErrorResponse(ErrorDetails.INTERNAL_ERROR_OCCURED.getErrorCode(), ex.getMessage());
        }
        return error;
    }

    private String[] getStringFormattedArgs(String[] split) {
        List<String> splittedData = new ArrayList<>();
        for(String filed: split) {
            splittedData.addAll(Arrays.asList(filed.split("\\,")));
        }
        return splittedData.toArray(new String[splittedData.size()]);
    }

//    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        BindingResult result = ex.getBindingResult();
        FieldError error = result.getFieldError();
        return new ResponseEntity<>(processFieldError(error), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<ErrorResponse> handleAll(Exception ex, WebRequest request) {
        logger.error(UNEXPECTED_ERROR, ex);
        ErrorResponse error = new ErrorResponse("10000", ex.getMessage());
        MDC.remove(TRACE_ID);
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ErrorResponse processFieldError(FieldError error) {
        ErrorResponse message = null;
        if(error != null) {
            String msg = messageSource.getMessage(error.getDefaultMessage(), null, currentLocale);
            message = new ErrorResponse(error.getDefaultMessage(), msg);
        }
        return message;
    }

    private String getErrorMessage(final String errorCode) {
        return messageSource.getMessage(errorCode, null, currentLocale);
    }
}