import type { ChangeEvent } from "react";
import type { ProductForm } from "../types/product";
import "./ProductEditPanel.css";

type ProductFormErrors = Partial<Record<keyof ProductForm, string>>;

type ProductEditPanelProps = {
  productId: number;
  form: ProductForm;
  errors: ProductFormErrors;
  isSaving: boolean;
  onChange: (event: ChangeEvent<HTMLInputElement>) => void;
  onSave: () => void;
  onCancel: () => void;
};

export function ProductEditPanel({
  productId,
  form,
  errors,
  isSaving,
  onChange,
  onSave,
  onCancel,
}: ProductEditPanelProps) {
  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onSave();
  }

  return (
    <div className="product-panel-backdrop" onClick={onCancel}>
      <aside
        className="product-panel"
        role="dialog"
        aria-modal="true"
        aria-labelledby="product-panel-title"
        onClick={(event) => event.stopPropagation()}
      >
        <header className="product-panel-header">
          <div>
            <p className="eyebrow">Product #{productId}</p>
            <h2 id="product-panel-title">Edit product</h2>
          </div>

          <button
            type="button"
            className="product-panel-close"
            onClick={onCancel}
            aria-label="Close edit panel"
          >
            ×
          </button>
        </header>

        <form className="product-panel-form" onSubmit={handleSubmit}>
          <label>
            Product name

            <input
              name="name"
              value={form.name}
              onChange={onChange}
              disabled={isSaving}
            />

            {errors.name && (
              <span className="field-error" role="alert">
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
              disabled={isSaving}
            />

            {errors.price && (
              <span className="field-error" role="alert">
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
              disabled={isSaving}
            />

            {errors.stock && (
              <span className="field-error" role="alert">
                {errors.stock}
              </span>
            )}
          </label>

          <footer className="product-panel-actions">
            <button
              type="button"
              className="button ghost"
              onClick={onCancel}
              disabled={isSaving}
            >
              Cancel
            </button>

            <button type="submit" className="button" disabled={isSaving}>
              {isSaving ? "Saving..." : "Save changes"}
            </button>
          </footer>
        </form>
      </aside>
    </div>
  );
}