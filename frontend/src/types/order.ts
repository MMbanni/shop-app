export type Order = {
       orderId: number;
       status: OrderStatus;
       paidAt: string | null
}

export type OrderStatus =
  | "PENDING"
  | "PAID"
  | "CANCELLED"
  | "EXPIRED"
  | "SUPERSEDED"
  | "NEEDS_REVIEW";
 