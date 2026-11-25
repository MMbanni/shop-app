export type Order = {
       orderId: number;
       status: OrderStatus;
       paidAt: string | null;
       reviewNeededAt: string | null
}

export type OrderItemDto = {
  productId: number;
  name: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
};

export type OrderDetails = {
  id: number;
  status: string; 
  createdAt: string; 
  paidAt: string | null; 
  total: number;
  items: OrderItemDto[];
};


export type OrderStatus =
  | "PENDING"
  | "PAID"
  | "CANCELLED"
  | "EXPIRED"
  | "SUPERSEDED";
 