import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { api } from "../lib/api";
import { AdminProductTab, ProductStatus } from "../types";
import { UpdateProductsRequest } from "../dto";


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
      mutationFn: ( product:UpdateProductsRequest) => api.adminUpdateProduct(product),  
      onSuccess: refreshProductLists
    });
  
    const changeProductStatus = useMutation({
      mutationFn: ({
        productId,
        status,
      }: { productId: number; status: ProductStatus }) => api.changeProductStatus(productId, status),
  
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
  
