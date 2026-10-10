package com.mbanni.shop.common;

import java.math.BigDecimal;
import java.time.Duration;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;

public class Constants {
    // CART
    public static final int CART_MAX_QUANTITY = 999;

    // PRODUCT
    public static final int MAX_STOCK = 9999;
    public static final int PRICE_PRECISION = 8;
    public static final String MIN_PRICE_SEK = "4.00";
    public static final String MAX_PRICE_SEK = "999999.99";

    // COMMON
    public static final int SCALE = 2;
    public static final int MIN_NAME_LENGTH = 2;
    public static final int MAX_NAME_LENGTH = 50;
    public static final int URL_MAX_LENGTH = 2048;

    // USER
    public static final int MAX_EMAIL_LENGTH = 254;
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 72;

    // ORDER
    public static final int ORDER_TOTAL_PRECISION = 12;
    public static final int LINE_TOTAL_PRECISION = 11;
    public static final BigDecimal MAX_ORDER_TOTAL = new BigDecimal("9999999999.99");

    // RECOVERY
    public static final Duration CREATION_RETRY_LIMIT = Duration.ofMinutes(5);
    public static final Duration HIGH_PRIORITY_RECOVERY_TIME = Duration.of(30, SECONDS);
    public static final Duration LOW_PRIORITY_RECOVERY_TIME = Duration.of(5, MINUTES);
    public static final Duration LEASE_DURATION = Duration.of(60, SECONDS);




}
