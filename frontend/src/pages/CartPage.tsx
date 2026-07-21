import { money } from "../lib/money";
import { useCart } from "../hooks/useCart";
import { ApiErrorResponse } from "../types";
import { getApiError } from "../lib/ApiError";


export function CartPage() {
  const {
    cartQuery,
    updateMutation,
    removeMutation,
    checkoutMutation,
  } = useCart();

  if (cartQuery.isLoading) {
    return <p className="page-message">Loading cart...</p>;
  }

  if (cartQuery.isError) {
    return (
      <p className="page-message error">
        {cartQuery.error.message}
      </p>
    );
  }

  function getProductErrorMessage(
    productName: string,
    error: ApiErrorResponse,
  ) {
    const availableStock = error.stock;

    if (error.title === "CART_ERROR") {
      if (availableStock === 0) {
        return (
          <>
            <b>{productName}</b> is out of stock.
          </>
        );
      }

      if (availableStock !== undefined) {
        return (
          <>
            Only {availableStock}{" "}
            {availableStock === 1 ? "unit" : "units"} of{" "}
            <b>{productName}</b>{" "}
            {availableStock === 1 ? "is" : "are"} available.
          </>
        );
      }
    }

    return error.detail ?? `Could not update ${productName}.`;
  }

  const cart = cartQuery.data;
  const isEmpty = !cart || cart.items.length === 0;

  const sortedItems = [...(cart?.items ?? [])].sort(
    (a, b) => a.cartItemId - b.cartItemId,
  );

  const updateError = getApiError(updateMutation.error);
  const removeError = getApiError(removeMutation.error);

  const checkoutError = getApiError(
    checkoutMutation.error,
  )

  /*
   * Only use checkout item errors when checkout is currently
   * in an error state. This prevents old checkout errors from
   * appearing after the cart changes.
   */
  const checkoutItemErrors =
    checkoutMutation.isError
      ? checkoutError?.itemErrors ??
        (checkoutError?.cartItemId !== undefined
          ? [checkoutError]
          : [])
      : [];

  const checkoutErrorsByItemId = new Map<
    number,
    ApiErrorResponse
  >();

  for (const error of checkoutItemErrors) {
    if (error.cartItemId !== undefined) {
      checkoutErrorsByItemId.set(
        error.cartItemId,
        error,
      );
    }
  }

  const hasCheckoutItemErrors =
    checkoutErrorsByItemId.size > 0;

  function updateQuantity(
    itemId: number,
    quantity: number,
  ) {
    /*
     * The cart is changing, so previous checkout and remove
     * errors are no longer relevant.
     */
    checkoutMutation.reset();
    removeMutation.reset();
    updateMutation.reset();

    updateMutation.mutate({
      itemId,
      quantity,
    });
  }

  function removeItem(itemId: number) {
    /*
     * Clear errors from previous actions before removing.
     */
    checkoutMutation.reset();
    updateMutation.reset();
    removeMutation.reset();

    removeMutation.mutate(itemId);
  }

  function checkout() {
    /*
     * Update/remove errors should not remain visible when
     * the user tries checkout again.
     */
    updateMutation.reset();
    removeMutation.reset();
    checkoutMutation.reset();

    checkoutMutation.mutate();
  }

  return (
    <main className="page-shell narrow">
      <div className="page-heading">
        <p className="eyebrow">Cart</p>
        <h1>Your cart</h1>
      </div>

      {isEmpty ? (
        <div className="empty-state">
          <h2>Your cart is empty</h2>
          <p>
            Add products first, then come back here to pay.
          </p>
        </div>
      ) : (
        <section className="cart-layout">
          <div className="cart-list">
            {sortedItems.map((item) => {
              const updateErrorForItem =
                updateMutation.isError &&
                updateError?.cartItemId === item.cartItemId
                  ? updateError
                  : null;

              const removeErrorForItem =
                removeMutation.isError &&
                removeError?.cartItemId === item.cartItemId
                  ? removeError
                  : null;

              const checkoutErrorForItem =
                checkoutErrorsByItemId.get(
                  item.cartItemId,
                ) ?? null;

              const itemError =
                updateErrorForItem ??
                removeErrorForItem ??
                checkoutErrorForItem;

              const isUpdatingThisItem =
                updateMutation.isPending &&
                updateMutation.variables?.itemId ===
                  item.cartItemId;

              const isRemovingThisItem =
                removeMutation.isPending &&
                removeMutation.variables ===
                  item.cartItemId;

              return (
                <article
                  className="cart-item"
                  key={item.cartItemId}
                >
                  <div>
                    <h3>{item.productName}</h3>

                    <p className="muted">
                      {money(item.price)} each
                    </p>

                    {itemError && (
                      <p className="error" role="alert">
                        {getProductErrorMessage(
                          item.productName,
                          itemError,
                        )}
                      </p>
                    )}
                  </div>

                  <div className="quantity-controls">
                    <button
                      className="round-button"
                      disabled={
                        isUpdatingThisItem ||
                        isRemovingThisItem
                      }
                      onClick={() =>
                        updateQuantity(
                          item.cartItemId,
                          -1,
                        )
                      }
                    >
                      -
                    </button>

                    <span>{item.quantity}</span>

                    <button
                      className="round-button"
                      disabled={
                        isUpdatingThisItem ||
                        isRemovingThisItem
                      }
                      onClick={() =>
                        updateQuantity(
                          item.cartItemId,
                          1,
                        )
                      }
                    >
                      +
                    </button>
                  </div>

                  <strong>
                    {money(item.lineTotal)}
                  </strong>

                  <button
                    className="button danger"
                    disabled={
                      removeMutation.isPending ||
                      isUpdatingThisItem
                    }
                    onClick={() =>
                      removeItem(item.cartItemId)
                    }
                  >
                    {isRemovingThisItem
                      ? "Removing..."
                      : "Remove"}
                  </button>
                </article>
              );
            })}
          </div>

          <aside className="summary-card">
            <h2>Order summary</h2>

            <div className="summary-row">
              <span>Total</span>
              <strong>{money(cart.total)}</strong>
            </div>

            <button
              className="button large full"
              onClick={checkout}
              disabled={
                checkoutMutation.isPending ||
                updateMutation.isPending ||
                removeMutation.isPending
              }
            >
              {checkoutMutation.isPending
                ? "Opening Stripe..."
                : "Pay with Stripe"}
            </button>

            {checkoutMutation.isError && (
              <p className="error" role="alert">
                {hasCheckoutItemErrors
                  ? "Please update the highlighted items before checkout."
                  : checkoutError?.detail ??
                    checkoutMutation.error.message}
              </p>
            )}
          </aside>
        </section>
      )}
    </main>
  );
}