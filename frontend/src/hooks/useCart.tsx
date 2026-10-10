import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { api } from "../lib/api";
import { useState } from "react";
import { CartItemProblem } from "../types";
import { getCartItemProblems, getApiError } from "../lib/ApiError";

export function useCart() {
  const queryClient = useQueryClient();
  const [checkoutProblems, setCheckoutProblems] = useState<CartItemProblem[]>([]);

  const cartQuery = useQuery({
    queryKey: ["cart"],
    queryFn: api.cart,
  });

  const updateMutation = useMutation({
    mutationFn: ({
      itemId,
      quantity,
    }: {
      itemId: number;
      quantity: number;
    }) => api.updateCartItem(itemId, quantity),

    onSuccess: async (_response, variables) => {
      try {
        const updatedCart = await api.cart();

        queryClient.setQueryData(["cart"], updatedCart);

        const updatedItem = updatedCart.items.find(
          (item) => item.cartItemId === variables.itemId
        );

        setCheckoutProblems((problems) =>
          problems.filter((problem) => {
            // Keep problems belonging to other items.
            if (problem.cartItemId !== variables.itemId) {
              return true;
            }

            // Decreasing to zero may have removed the item.
            if (!updatedItem) {
              return false;
            }

            // Clear the stock warning if the quantity now fits.
            if (
              problem.code === "INSUFFICIENT_STOCK" &&
              typeof problem.stock === "number"
            ) {
              return updatedItem.quantity > problem.stock;
            }

            // Keep price and availability problems.
            return true;
          })
        );
      } catch {
        // Update succeeded but refreshing failed.
        // Keep the warnings and request another cart refresh.
        return queryClient.invalidateQueries({
          queryKey: ["cart"],
        });
      }
    },
  });

  const removeMutation = useMutation({
    mutationFn: (itemId: number) => api.removeCartItem(itemId),

    onSuccess: (_response, itemId) => {
      setCheckoutProblems((problems) =>
        problems.filter(
          (problem) => problem.cartItemId !== itemId
        )
      );

      return queryClient.invalidateQueries({
        queryKey: ["cart"],
      });
    },
  });

  const checkoutMutation = useMutation({
    mutationFn: api.createCheckout,

    onSuccess: (response) => {
      setCheckoutProblems([]);
      window.location.href = response.checkoutUrl;
    },
    onError: (error) => {
      setCheckoutProblems(getCartItemProblems(error));

      if (
        getApiError(error)?.title === "CHECKOUT_VALIDATION_FAILED"
      ) {
        return queryClient.invalidateQueries({
          queryKey: ["cart"],
        });
      }
    }
  });

  const confirmPrice = useMutation({
    mutationFn: ({ cartItemId, agreedPrice }: { cartItemId: number, agreedPrice: number }) => api.confirmPrice(cartItemId, agreedPrice),

    onSuccess: (_response, variables) => {

      checkoutMutation.reset();
      setCheckoutProblems((problems) =>
        problems.filter(
          (problem) =>
            !(
              problem.cartItemId === variables.cartItemId &&
              problem.code === "PRICE_CHANGED"
            )
        )
      );

      return queryClient.invalidateQueries({
        queryKey: ["cart"],
      });
    },

    onError: (error) => {
      if (getApiError(error)?.title === "PRICE_CHANGED") {
        return queryClient.invalidateQueries({
          queryKey: ["cart"],
        });
      }
    },
  });

  return {
    cartQuery,
    updateMutation,
    removeMutation,
    checkoutMutation,
    confirmPrice,
    checkoutProblems
  };
}