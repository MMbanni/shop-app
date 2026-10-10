export type CartItem = {
  cartItemId: number;
  productId: number;
  productName: string;
  quantity: number;
  priceWhenAdded: number
  price: number;
  priceChanged: boolean;
  lineTotal: number;
};

export type Cart = {
  items: CartItem[];
  total: number;
};