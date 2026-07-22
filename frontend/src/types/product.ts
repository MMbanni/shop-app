export type Product = {
  id: number;
  name: string;
  description?: string;
  price: number;
  imageUrl?: string;
  stock: number;
  status: string
};

export type ProductStatus = "ACTIVE" | "INACTIVE" | "ARCHIVED";

export type ProductForm = {
  name: string;
  price: string;
  stock: string;
};