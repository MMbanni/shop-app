package com.mbanni.shop.checkout;

import com.mbanni.shop.cart.dto.CartItemProblem;

import java.util.List;

public class CheckoutValidationException extends RuntimeException {

  private final List<CartItemProblem> itemErrors;

  public CheckoutValidationException(List<CartItemProblem> itemErrors) {
    super("Some cart items cannot be checked out.");
    this.itemErrors = List.copyOf(itemErrors);
  }

  public List<CartItemProblem> getErrors() {
    return itemErrors;
  }
}