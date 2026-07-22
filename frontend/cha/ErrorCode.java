package com.mbanni.shop.common.exception;

public enum ErrorCode {

    USER_NOT_FOUND(
            "User not found"
    ),
    EMAIL_ALREADY_USED(
            "Email is already in use"
    ),

    INVALID_CREDENTIALS(
            "Incorrect email or password"
    ),

    ACCESS_DENIED(
            "You do not have permission to perform this action"
    ),

    ACCOUNT_SUSPENDED(
            "This account is suspended"
    ),

    ACCOUNT_BANNED(
            "This account is banned"
    ),

    PRODUCT_NOT_FOUND(
            "Product not found"
    ),

    PRODUCT_ALREADY_EXISTS(
            "Product already exists"
    ),

    PRODUCT_NOT_IN_CART(
            "Product is not in cart"
    ),

    CART_ERROR(
            "Some items in your cart need attention before checkout"
    ),

    CART_ITEM_NOT_FOUND(
            "Product not found"
    ),

    ILLEGAL_OPERATION(

    ),

    EXCEEDED_QUANTITY_LIMIT(
            "Maximum quantity is 999"
    ),



    ORDER_NOT_FOUND(
            "Order not found"
    ),

    TOO_MANY_ATTEMPTS(
            "Too many attempts"
    );

    private String defaultMessage;

    ErrorCode(){}

    ErrorCode(String message){
        this.defaultMessage =message;
    };

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public void setDefaultMessage(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }
}