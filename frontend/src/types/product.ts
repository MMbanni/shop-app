export type Product = {
  id: number;
  name: string;
  description?: string | null;
  price: number;
  imageUrl?: string;
  attributes: Record<string, string>;
  stock: number;
  status: ProductStatus;
};

export type AdminProduct = Product & {
  version: number;
};

export type ProductStatus = "ACTIVE" | "INACTIVE" | "ARCHIVED";

export type ProductForm = {
  name: string;
  price: string;
  stock: string;
  description?: string,
  imageUrl?: string,
  attributes: Record<string,string>
};

export type ProductFormErrors = Partial<Record<keyof ProductForm, string>>;
