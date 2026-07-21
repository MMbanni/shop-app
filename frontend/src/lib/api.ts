import type { ApiErrorResponse, AdminProductTab, Cart, Product, ProductStatus, User } from "../types";
import type { AddProductsRequest, UpdateProductsRequest, CheckoutResponse,  LoginResponse} from "../dto/";

import { getToken } from "./token";
import { ApiError } from "./ApiError";

const API_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken();
  const headers = new Headers(options.headers); // Current headers

  // Make sure it's not file upload
  if (options.body && !(options.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }

  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers
  });
  
  // No content
  if (response.status === 204) {
    return undefined as T;
  }
  
  // Fallback
  if (!response.ok) {
    let errorResponse: ApiErrorResponse = {
    title: "REQUEST_FAILED",
    detail: `Request failed with status ${response.status}`,
    status: response.status
  };

    try {
      const data = (await response.json()) as ApiErrorResponse;
      errorResponse = {
        ...errorResponse,
        ...data
      };
    } catch {
      // fallback if backend did not return JSON.
    }
    
    console.log(errorResponse);
    
    throw new ApiError(errorResponse);
  }

  return response.json() as Promise<T>;
}



export const api = {
  login(email: string, password: string) {
    return request<LoginResponse>("/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password })
    });
  },

  register(name: string, email: string, password: string) {
    return request<User>("/auth/register", {
      method: "POST",
      body: JSON.stringify({ name, email, password })
    });
  },

  me() {
    return request<User>("/users/me");
  },

  users() {
    return request<User[]>("/users");
  },

  products() {
    return request<Product[]>("/products");
  },

  cart() {
    return request<Cart>("/cart");
  },

  addToCart(productId: number, quantity = 1) {
    return request<Cart>("/cart", {
      method: "POST",
      body: JSON.stringify({ productId, quantity })
    });
  },

  updateCartItem(itemId: number, quantity: number) {
    return request<Cart>(`/cart/items/${itemId}`, {
      method: "PUT",
      body: JSON.stringify(quantity)
    });
  },

  removeCartItem(itemId: number) {
    return request<void>(`/cart/items/${itemId}`, {
      method: "DELETE"
    });
  },

  createCheckout() {
    return request<CheckoutResponse>("/payments/checkout", {
      method: "POST"
    });
  },

  cancelCheckout() {
    return request<void>("/payments/checkout/cancel", {
      method: "POST"
    });
  },


  adminRemoveProduct(productId: number) {
    return request<void>(`/admin/products/${productId}`, {
      method: "DELETE"
    });
  },

  adminAddProduct(props: AddProductsRequest) {
    return request<void>(`/admin/products`, {
      method: "POST",
      body: JSON.stringify(props)
    });
  },

  adminUpdateProduct(props: UpdateProductsRequest) {
    return request<void>(`/admin/products/${props.id}`, {
      method: "PATCH",
      body: JSON.stringify(props)
    });
  },

  adminGetProducts(status: AdminProductTab) {
    const query = status === "ALL" ? "" : `?status=${status}`;

    return request<Product[]>(`/admin/products${query}`);
  },

  changeProductStatus(productId: number, status: ProductStatus) {
    return request<void>(`/admin/products/${productId}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status })
    });
  }

  
};

