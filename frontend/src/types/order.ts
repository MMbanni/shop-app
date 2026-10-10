export type Order = {
       orderId: number;
       status: OrderStatus;
       paidAt: string | null;
       reviewNeededAt: Date | null
}

export type OrderStatus =
  | "PENDING"
  | "PAID"
  | "CANCELLED"
  | "EXPIRED"
  | "SUPERSEDED";
 