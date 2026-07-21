import { getApiError, getFieldErrors } from "../../lib/ApiError";

type ApiErrorMessageProps = {
  error: unknown;
  fallback: string;
};

export function ApiErrorMessage({error,fallback}: ApiErrorMessageProps) {
  
  const response = getApiError(error);
  const fieldErrors = getFieldErrors(error);

  return (
    <div className="error" role="alert">
      <p>{response?.detail ?? fallback}</p>

      {fieldErrors && (
        <ul>
          {Object.entries(fieldErrors).map(([field, message]) => (
            <li key={field}>
              <strong>{field}:</strong> {message}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}