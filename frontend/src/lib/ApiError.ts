import type {
  ApiErrorResponse,
  CartItemProblem,
  ValidationFieldError
} from "../types";

export class ApiError extends Error {
  readonly response: ApiErrorResponse;

  constructor(response: ApiErrorResponse) {
    const message =
      response.detail ||
      response.title ||
      `Request failed with status ${response.status}`;

    super(message);

    this.name = "ApiError";
    this.response = response;
  }
}

export function getApiError(error: unknown): ApiErrorResponse | null {
  return error instanceof ApiError ? error.response : null
}

function isValidationFieldError(
  value: unknown
): value is ValidationFieldError {
  return (
    typeof value === "object" &&
    value !== null &&
    "field" in value &&
    typeof value.field === "string" &&
    "message" in value &&
    typeof value.message === "string"
  );
}

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
      result[fieldError.field] ??= fieldError.message;
      return result;
    }, {});
}

export function getFormError(error: unknown): string | null {
  const response = getApiError(error);

  if (!response) {
    return error instanceof Error
      ? error.message
      : "An unexpected error occurred";
  }

  if (Object.keys(getFieldErrors(error)).length > 0) {
    return null;
  }

  return response.detail || response.title || "Request failed";
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
