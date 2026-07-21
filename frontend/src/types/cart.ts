export type CartItem = {
  cartItemId: number;
  productId: number;
  productName: string;
  quantity: number;
  price: number;
  lineTotal: number;
};

export type Cart = {
  items: CartItem[];
  total: number;
};