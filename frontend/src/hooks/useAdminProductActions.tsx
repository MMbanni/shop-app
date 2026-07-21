import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { api } from "../lib/api";
import { useState } from "react";
import { AdminProductTab, ProductStatus } from "../types";

type ProductInput = {
  name: string;
  price: number;
  stock: number;
};

type UpdateProductInput = ProductInput & {
  id: number;
};

type ChangeStatusInput = {
  productId: number;
  status: ProductStatus;
};

export function useAdminProducts(selectedTab: AdminProductTab){
  const queryClient = useQueryClient();

  function refreshProductLists() {
    return Promise.all([
      queryClient.invalidateQueries({ queryKey: ["admin-products"] }),
      queryClient.invalidateQueries({ queryKey: ["products"] }),
    ]);
  }

  const adminProductsQuery=useQuery({
    queryKey: ["admin-products", selectedTab],
    queryFn: () => api.adminGetProducts(selectedTab),
  });

  const addProduct = useMutation({
      mutationFn: api.adminAddProduct,
      onSuccess: refreshProductLists,
    });
  
    const updateProduct = useMutation({
      mutationFn: ( product:UpdateProductInput) => api.adminUpdateProduct(product),  
      onSuccess: refreshProductLists
    });
  
    const changeProductStatus = useMutation({
      mutationFn: ({
        productId,
        status,
      }: ChangeStatusInput) => api.changeProductStatus(productId, status),
  
      onSuccess: refreshProductLists
    });
  
    const removeProduct = useMutation({
      mutationFn: (productId: number) => api.adminRemoveProduct(productId),
      onSuccess: refreshProductLists
    });

  return {
    adminProductsQuery,
    addProduct,
    updateProduct,
    changeProductStatus,
    removeProduct
  }

}
  
