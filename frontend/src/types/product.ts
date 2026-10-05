export type Product = {
  id: number;
  name: string;
  description?: string | null;
  price: number;
  imageUrl?: string;
  stock: number;
  status: ProductStatus;
  version: number
};

export type ProductStatus = "ACTIVE" | "INACTIVE" | "ARCHIVED";

export type ProductForm = {
  name: string;
  price: string;
  stock: string;
};