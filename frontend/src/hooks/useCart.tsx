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

    onSuccess: () => {
      return queryClient.invalidateQueries({
        queryKey: ["cart"],
      });
    },
  });

  const removeMutation = useMutation({
    mutationFn: (itemId: number) => api.removeCartItem(itemId),

    onSuccess: () => {
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
    mutationFn: ({cartItemId, agreedPrice}:{cartItemId:number, agreedPrice:number})=>api.confirmPrice(cartItemId, agreedPrice),

    onSuccess: () => {
      checkoutMutation.reset();
      return queryClient.invalidateQueries({
        queryKey: ["cart"],
      });
    },
  });

  return {
    cartQuery,
    updateMutation,
    removeMutation,
    checkoutMutation,
    confirmPrice
  };
}