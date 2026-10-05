export type ValidationFieldError = {
  field: string;
  code: string;
  message: string;
};

export type CartItemProblem = {
  code: string;
  cartItemId: number | null;
  productId?: number;
  stock?: number | null;
  title?: string;
  detail?: string;
  priceChanged?:boolean
};

export type ApiErrorResponse = {
  title: string;
  detail: string;
  status: number;

  errors?: ValidationFieldError[];
  itemErrors?: CartItemProblem[]
  };