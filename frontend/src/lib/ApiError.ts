import type {
  ApiErrorResponse,
  CartItemProblem,
  ValidationFieldError
} from "../types";

/**
 * Error thrown when the API returns a structured error response.
 * Extends Error class to contain full API response.
 */
export class ApiError extends Error {
  readonly response: ApiErrorResponse;

  constructor(response: ApiErrorResponse) {
    const message =
      response.detail ??
      response.title ??
      `Request failed with status ${response.status}`;

    super(message);

    this.name = "ApiError";
    this.response = response;
  }
}

/**
 * Extracts structured API response from an unknown caught value.
 * Returns null if the value was not created as an ApiError.
 */

export function getApiError(error: unknown): ApiErrorResponse | null {
  return error instanceof ApiError ? error.response : null
}

/**
 * Runtime type guard for validation errors returned by the API.
 *
 * This is required because TypeScript types do not validate JSON received
 * from external systems at runtime.
 */

function isValidationFieldError(
  value: unknown
): value is ValidationFieldError {
  return (
    value !== null &&
    typeof value === "object" &&    
    "field" in value &&
    typeof value.field === "string" &&
    "code" in value &&
    typeof value.code === "string" &&
    "message" in value &&
    typeof value.message === "string"
  );
}

/**
 * Converts the API's validation-error array into a field-to-message lookup.
 *
 * Example:
 * [{ field: "name", message: "Name required" }]
 * becomes:
 * { price: "Price must be positive" }
 */
export function getFieldErrors(
  error: unknown
): Record<string, string> {
  const errors = getApiError(error)?.errors;

  if (!Array.isArray(errors)) {
    return {};
  }

  return errors
    .filter(isValidationFieldError)
    .reduce<Record<string, string>>((result, fieldError) => {
      result[fieldError.field] ??= fieldError.message; // Keep first message if field fails multiple rules
      return result;
    }, {});
}

export function getErrorMessage(error: unknown): string {
  const response = getApiError(error);

  if (response) {
    return response.detail || response.title || "Request failed";
  }

  return error instanceof Error
    ? error.message
    : "An unexpected error occurred";
}

export function getFormErrorMessage(
  error: unknown
): string | null {
  const fieldErrors = getFieldErrors(error);

  if (Object.keys(fieldErrors).length > 0) {
    return null;
  }

  return getErrorMessage(error);
}

function isCartItemProblem(
  value: unknown
): value is CartItemProblem {

  if (value === null || typeof value !== "object") {
    return false;
  }

  const problem = value as Record<string, unknown>;
  const hasCartItemId = typeof problem.cartItemId === "number";
  const hasProductId = typeof problem.productId === "number";

  return (
    typeof problem.code === "string" &&
    typeof problem.detail === "string" &&
    (hasCartItemId || hasProductId)
  );
}

export function getCartItemProblems(error: unknown): CartItemProblem[] {
  const response = getApiError(error);

  if (!response) {
    return [];
  }

  if (Array.isArray(response.itemErrors)) {
    return response.itemErrors.filter(isCartItemProblem);
  }

  if (isCartItemProblem(response)) {
    return [response];
  }

  return [];
}
