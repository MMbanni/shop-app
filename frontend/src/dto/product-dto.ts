export type AddProductsRequest = {
  name: string;
  price: number;
  stock?: number;
  description?: string;
  images?: string[];
  attributes?: Record<string, string>;
};

export type UpdateProductsRequest = {
  id: number;
  expectedVersion: number;
  name?: string;
  price?: number;
  stock?: number;
  description?: string;
  images?: string[];
  attributes: Record<string,string>
};