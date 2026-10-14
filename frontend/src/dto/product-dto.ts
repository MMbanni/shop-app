export type AddProductsRequest = {
  name: string;
  price: number;
  stock?: number
}

export type UpdateProductsRequest = {
  id: number;
  expectedVersion: number;
  name?: string;
  price?: number;
  stock?: number
}