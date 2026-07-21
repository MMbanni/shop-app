export type AddProductsRequest = {
  name: string;
  price: number;
  stock?: number
}

export type UpdateProductsRequest = {
  id: number
  name?: string;
  price?: number;
  stock?: number
}