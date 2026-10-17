export type AddProductsRequest = {
  name: string;
  price: number;
  stock?: number;
  description?: string;
  imageUrl?: string
}

export type UpdateProductsRequest = {
  id: number;
  expectedVersion: number;
  name?: string;
  price?: number;
  stock?: number;
  description?: string;
  imageUrl?: string
}