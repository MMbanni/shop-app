export type ValidationFieldError = {
  field: string;
  code: string | null;
  message: string;
};

export type CartItemProblem = {
  cartItemId: number;
  productId?: number;
  stock?: number;
  title?: string;
  detail?: string;
};

export type ApiErrorResponse = {
  title: string;
  detail: string;
  status: number;

  errors?: ValidationFieldError[];
  itemErrors?: CartItemProblem[]

  cartItemId?: number;
  productId?: number;
  stock?: number;
};

export type ValidationProblem =
  Omit<ApiErrorResponse, "errors"> & {
    errors: ValidationFieldError[];
  };


export type CheckoutProblem =
  Omit<ApiErrorResponse, "itemErrors"> & {
    errors?: CartItemProblem[];
  };