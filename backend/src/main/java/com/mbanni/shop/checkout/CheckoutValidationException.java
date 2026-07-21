package com.mbanni.shop.checkout;

import java.util.List;

public class CheckoutValidationException extends RuntimeException {

  private final List<CheckoutItemError> itemErrors;

  public CheckoutValidationException(List<CheckoutItemError> itemErrors) {
    super("Some cart items cannot be checked out.");
    this.itemErrors = List.copyOf(itemErrors);
  }

  public List<CheckoutItemError> getErrors() {
    return itemErrors;
  }
}