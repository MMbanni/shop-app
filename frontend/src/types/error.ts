export type ApiErrorResponse = {
  title: string;
  detail: string;
  status: number;

  errors?: unknown;
  cartItemId?: number;
  productId?: number;
  stock?: number;
  itemErrors?: CartItemProblem[]
  
};

type ProblemDetails = {
  title?: string;
  detail?: string;
  status?: number;
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