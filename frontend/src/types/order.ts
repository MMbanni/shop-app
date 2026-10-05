export type Order = {
       orderId: number;
       status: OrderStatus;
       paidAt: Date
}

export type OrderStatus =
  | "PENDING"
  | "PAID"
  | "CANCELLED"
  | "EXPIRED"
  | "NEEDS_REVIEW";
 