package com.mbanni.shop.common.exception;

import com.mbanni.shop.checkout.CheckoutValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        HttpStatus status = statusFor(errorCode);

        // Constructor is protected so we use static factory method
        ProblemDetail problem = ProblemDetail.forStatus(status);


        problem.setTitle(errorCode.name());
        problem.setDetail(ex.getMessage());

        ex.getProperties().forEach(problem::setProperty);

        return ResponseEntity
                .status(status)
                .body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(
            MethodArgumentNotValidException ex
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

        problem.setTitle("VALIDATION_ERROR");
        problem.setDetail("Invalid request body");

        List<ValidationFieldError> errors =
                ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(fieldError -> new ValidationFieldError(
                                fieldError.getField(),
                                fieldError.getCode(),
                                fieldError.getDefaultMessage()
                        ))
                        .toList();

        problem.setProperty("errors", errors);

        return ResponseEntity
                .badRequest()
                .body(problem);
    }

    @ExceptionHandler(CheckoutValidationException.class)
    public ResponseEntity<ProblemDetail>
    handleCheckoutValidationException( CheckoutValidationException ex) {
        HttpStatus status = HttpStatus.UNPROCESSABLE_CONTENT;

        ProblemDetail problem = ProblemDetail.forStatus(status);

        problem.setTitle("CHECKOUT_VALIDATION_FAILED");
        problem.setDetail(ex.getMessage());
        problem.setProperty("itemErrors", ex.getErrors());



        return ResponseEntity
                .status(status)
                .body(problem);
    }

    private HttpStatus statusFor(ErrorCode errorCode) {
        return switch (errorCode) {
            case USER_NOT_FOUND,
                 PRODUCT_NOT_FOUND,
                 CART_ITEM_NOT_FOUND,
                 ORDER_NOT_FOUND-> HttpStatus.NOT_FOUND;

            case EMAIL_ALREADY_USED,
                 PRODUCT_ALREADY_EXISTS-> HttpStatus.CONFLICT;

            case INVALID_CREDENTIALS -> HttpStatus.UNAUTHORIZED;

            case ACCESS_DENIED,
                 ACCOUNT_BANNED,
                 ACCOUNT_SUSPENDED-> HttpStatus.FORBIDDEN;


            case CART_ERROR -> HttpStatus.UNPROCESSABLE_CONTENT;

            case EXCEEDED_QUANTITY_LIMIT,
                 ILLEGAL_OPERATION,
                 PRODUCT_NOT_IN_CART -> HttpStatus.BAD_REQUEST;

            case TOO_MANY_ATTEMPTS -> HttpStatus.TOO_MANY_REQUESTS;
        };
    }
}