import { money } from "../lib/money";
import { useCart } from "../hooks/useCart";
import { CartItemProblem } from "../types";
import { getApiError, getCartItemProblems, getErrorMessage } from "../lib/ApiError";
import { Confirm } from "../components/messages/Confirm";

// { CartItem ID: Problem }
function mapProblemsByCartItemId(problems: CartItemProblem[]): Map<number, CartItemProblem> {
  const problemsByItemId = new Map<number, CartItemProblem>();

  for (const problem of problems) {
    if (problem.cartItemId !== undefined) {
      problemsByItemId.set(
        problem.cartItemId,
        problem,
      );
    }
  }
  return problemsByItemId;
}

export function CartPage() {
  const {
    cartQuery,
    updateMutation,
    removeMutation,
    checkoutMutation,
    confirmPrice
  } = useCart();

  const isCartBusy =
    updateMutation.isPending ||
    removeMutation.isPending ||
    checkoutMutation.isPending ||
    confirmPrice.isPending;

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

  const updateItemErrors = getCartItemProblems(updateMutation.error);
  const removeItemErrors = getCartItemProblems(removeMutation.error);
  const checkoutItemErrors = getCartItemProblems(checkoutMutation.error);

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
      return updateMutation.error.message;
    }

    if (removeMutation.isError && removeItemErrors.length === 0) {
      return removeMutation.error.message;
    }

    return null;
  };

  const generalActionError = getGeneralActionError();

  function updateQuantity(
    itemId: number,
    quantity: number,
  ) {
    if(isCartBusy) {
      return;
    }
    checkoutMutation.reset();
    removeMutation.reset();
    confirmPrice.reset();

    updateMutation.mutate({
      itemId,
      quantity,
    });
  }

  function removeItem(itemId: number) {
    if(isCartBusy) {
      return;
    }
    
    // Clear errors from previous actions before removing.    
    checkoutMutation.reset();
    updateMutation.reset();
    confirmPrice.reset();

    removeMutation.mutate(itemId);
  }

  function checkout() {

    if(isCartBusy) {
      return;
    }
    updateMutation.reset();
    removeMutation.reset();
    confirmPrice.reset();

    checkoutMutation.mutate();
  }

  return (
    <main className="page-shell narrow">
      <div className="page-heading">
        {/*<p className="section-label">Cart</p>*/}
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

      {confirmPrice.isError && (
        <p className="page-message error" role="alert">
          {getErrorMessage(confirmPrice.error)}
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
                      itemError.code === "PRICE_CHANGED" ? (

                        <Confirm
                          message={`The price of this item has changed from ${item.priceWhenAdded} to ${item.price}. Would you like to proceed with the current price?`}
                          onConfirm={() => confirmPrice.mutate({cartItemId: item.cartItemId, agreedPrice:item.price})}
                          disabled={isCartBusy} >


                        </Confirm>
                      ) : (

                        <p className="error" role="alert">
                          {getProductErrorMessage(
                            item.productName,
                            itemError,
                          )}
                        </p>
                      )
                    )
                    }
                  </div>

                  <div className="quantity-controls">
                    <button
                      className="round-button"
                      disabled={isCartBusy}
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
                      disabled={isCartBusy}
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
                    disabled={isCartBusy}
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
              disabled={isCartBusy}
            >
              {checkoutMutation.isPending
                ? "Opening Stripe..."
                : "Pay with Stripe"}
            </button>

            {checkoutMutation.isError && (
              <p className="error" role="alert">
                {hasCheckoutItemErrors
                  ? "Please update the highlighted items before checkout."
                  : getErrorMessage(checkoutMutation.error)}
              </p>
            )}
          </aside>
        </section>
      )}
    </main>
  );
}