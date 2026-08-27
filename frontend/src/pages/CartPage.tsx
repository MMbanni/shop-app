import { money } from "../lib/money";
import { useCart } from "../hooks/useCart";
import { ApiErrorResponse, CartItemProblem } from "../types";
import { getApiError } from "../lib/ApiError";

function mapProblemsByCartItemId( problems: CartItemProblem[]): Map<number, CartItemProblem> {
  const problemsByItemId = new Map<number, CartItemProblem>();

  for (const problem of problems) {
    if (problem.cartItemId !== undefined) {
      problemsByItemId.set(
        problem.cartItemId,
        problem,
      );
    }
  }

  console.log(problemsByItemId);
  

  return problemsByItemId;
}

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
    error: CartItemProblem,
  ) {
    const availableStock = error.stock;

    if (error.code === "INSUFFICIENT_STOCK") {
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

  const updateError =
    getApiError(updateMutation.error);

  const removeError =
    getApiError(removeMutation.error);

  const checkoutError =
    getApiError(checkoutMutation.error);

  const updateItemErrors =
    updateMutation.isError
      ? updateError?.itemErrors ?? []
      : [];

  const removeItemErrors =
    removeMutation.isError
      ? removeError?.itemErrors ?? []
      : [];

  const checkoutItemErrors =
    checkoutMutation.isError
      ? checkoutError?.itemErrors ?? []
      : [];

  const updateErrorsByItemId =
    mapProblemsByCartItemId(updateItemErrors);

  const removeErrorsByItemId =
    mapProblemsByCartItemId(removeItemErrors);

  const checkoutErrorsByItemId =
    mapProblemsByCartItemId(checkoutItemErrors);

  const hasCheckoutItemErrors =
    checkoutErrorsByItemId.size > 0;

  /*
* An update/remove error that cannot be connected
* to a particular cart item.
*/

  const getGeneralActionError = () => {
    if (updateMutation.isError && updateItemErrors.length === 0) {
      return updateError?.detail ?? updateMutation.error.message;
    }

    if (removeMutation.isError && removeItemErrors.length === 0) {
      return removeError?.detail ?? removeMutation.error.message;
    }

    return null;
  };

  const generalActionError = getGeneralActionError();

  function updateQuantity(
    itemId: number,
    quantity: number,
  ) {
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

    //Errors should not remain visible when user tries checkout again.
    updateMutation.reset();
    removeMutation.reset();
    checkoutMutation.reset();

    checkoutMutation.mutate();
  }

  return (
    <main className="page-shell narrow">
      <div className="page-heading">
        <p className="section-label">Cart</p>
        <h1>Your cart</h1>
      </div>

      {generalActionError && (
        <p
          className="page-message error"
          role="alert"
        >
          {generalActionError}
        </p>
      )}

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
                updateErrorsByItemId.get(
                  item.cartItemId,
                ) ?? null;

              const removeErrorForItem =
                removeErrorsByItemId.get(
                  item.cartItemId,
                ) ?? null;

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
            }
            )

            }
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