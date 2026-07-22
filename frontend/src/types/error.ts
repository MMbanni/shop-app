export type ApiErrorResponse = {
  title: string;
  detail: string;
  status: number;

  errors?: [Record<string,string>];
  cartItemId?: number;
  productId?: number;
  stock?: number;
  itemErrors?: CartItemProblem[]
  
};


export type ValidationProblem =
  Omit<ApiErrorResponse, "errors"> & {
    errors?: Record<string, string>;
  };

export type CartItemProblem =
  Omit<
    ApiErrorResponse,
    "errors" | "cartItemId"
  > & {
    cartItemId: number;
    productId?: number;
    stock?: number;
  };

export type CheckoutProblem =
  Omit<ApiErrorResponse, "errors"> & {
    errors?: CartItemProblem[];
  };