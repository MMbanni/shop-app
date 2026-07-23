import type {
  ApiErrorResponse, 
  CartItemProblem } from "../types";

export class ApiError extends Error {
  readonly response: ApiErrorResponse;

  constructor(response: ApiErrorResponse) {
    const message =
      response.detail ??
      response.title ??
      `Request failed with status ${response.status}` ;

    super(message);

    this.name = "ApiError";
    this.response = response;
  }
}

export function getApiError( error: unknown): ApiErrorResponse | null {
  if (error instanceof ApiError) {
    return error.response;
  }

  return null;
}

export function getFieldErrors(
  error: unknown
): Record<string, string> | null {
  const errors = getApiError(error)?.errors;

  if (
    typeof errors !== "object" ||
    errors === null ||
    Array.isArray(errors)
  ) {
    return null;
  }

  const entries = Object.entries(
    errors as Record<string, unknown>
  );

  if (!entries.every(([, value]) => typeof value === "string")) {
    return null;
  }

  return Object.fromEntries(entries) as Record<string, string>;
}

function isCartItemProblem(
  value: unknown
): value is CartItemProblem {
  return (
    typeof value === "object" &&
    value !== null &&
    "cartItemId" in value &&
    typeof value.cartItemId === "number"
  );
}

export function getCartItemProblems(
  error: unknown
): CartItemProblem[] {
  const response = getApiError(error);

  if (!response) {
    return [];
  }

  if (Array.isArray(response.errors)) {
    return response.errors.filter(isCartItemProblem);
  }

  if (isCartItemProblem(response)) {
    return [response];
  }

  return [];
}
