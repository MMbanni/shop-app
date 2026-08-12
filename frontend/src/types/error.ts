export type ValidationFieldError = {
  field: string;
  code: string;
  message: string;
};

export type CartItemProblem = {
  code: string;
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
  };