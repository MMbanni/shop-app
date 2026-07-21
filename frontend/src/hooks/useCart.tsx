import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { api } from "../lib/api";

export function useCart() {
  const queryClient = useQueryClient();

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
      window.location.href = response.checkoutUrl;
    },
  });

  return {
    cartQuery,
    updateMutation,
    removeMutation,
    checkoutMutation,
  };
}