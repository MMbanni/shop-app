import type { ChangeEvent } from "react";
import type { ProductForm } from "../../types/product";
import "./ProductFormModal.css";

type ProductFormErrors = Partial<Record<keyof ProductForm, string>>;

type ProductFormModalProps = {
  title: string;
  form: ProductForm;
  errors: ProductFormErrors;
  submitError?: string | null
  isSubmitting: boolean;
  onChange: (event: ChangeEvent<HTMLInputElement>) => void;
  onSubmit: () => void;
  onClose: () => void;
};

export function ProductFormModal({
  title,
  form,
  errors,
  submitError,
  isSubmitting,
  onChange,
  onSubmit,
  onClose,
}: ProductFormModalProps) {
  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onSubmit();
  }

  return (
    <div
      className="modal-backdrop"
      onPointerDown={(event) => {
        if (event.target === event.currentTarget) {
          onClose();
        }
      }}
    >

      <aside
        className="product-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="product-modal-title"
        aria-describedby={
          submitError ? "product-modal-submit-error" : undefined
        }
      >
        <header className="product-modal-header">
          <h2 id="product-modal-title">{title}</h2>

          <button
            type="button"
            className="modal-close"
            onClick={onClose}
            aria-label="Close modal"
          >
            ×
          </button>
        </header>

        <form
          className="product-modal-form"
          onSubmit={handleSubmit}
          noValidate>
          
          {submitError && (
            <div
              id="product-modal-submit-error"
              className="product-modal-error"
              role="alert"
            >
              {submitError}
            </div>
          )}


          <label>
            Product name

            <input
              name="name"
              value={form.name}
              onChange={onChange}
              disabled={isSubmitting}
              aria-invalid={Boolean(errors.name)}
              aria-describedby={
                errors.name ? "product-name-error" : undefined
              }
            />

            {errors.name && (
              <span 
              id="product-name-error"
              className="field-error"
              role="alert"
              >
                {errors.name}
              </span>
            )}
          </label>

          <label>
            Price

            <input
              name="price"
              type="number"
              min="0"
              step="0.01"
              value={form.price}
              onChange={onChange}
              disabled={isSubmitting}
              aria-invalid={Boolean(errors.price)}
              aria-describedby={
                errors.price ? "product-price-error" : undefined
              }
            />

            {errors.price && (
              <span
                id="product-price-error"
                className="field-error"
                role="alert"
              >
                {errors.price}
              </span>
            )}
          </label>

          <label>
            Stock

            <input
              name="stock"
              type="number"
              min="0"
              step="1"
              value={form.stock}
              onChange={onChange}
              disabled={isSubmitting}
              aria-invalid={Boolean(errors.stock)}
              aria-describedby={
                errors.stock ? "product-stock-error" : undefined
              }
            />

            {errors.stock && (
              <span
                id="product-stock-error"
                className="field-error"
                role="alert"
              >
                {errors.stock}
              </span>
            )}
          </label>

          <footer className="product-modal-actions">
            <button
              type="button"
              className="button ghost"
              onClick={onClose}
              disabled={isSubmitting}
            >
              Cancel
            </button>

            <button 
            type="submit" 
            className="button" 
            disabled={isSubmitting}
            >
              {isSubmitting ? "Saving..." : "Save changes"}
            </button>
          </footer>
        </form>
      </aside>
    </div>
  );
}