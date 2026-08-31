import type { ChangeEvent } from "react";
import "./ProductFormModal.css";


type UserFormModalProps = {
    duration: string;
  submitError?: string | null
  isSubmitting: boolean;
  onChange: (event: ChangeEvent<HTMLInputElement>) => void;
  onSubmit: () => void;
  onClose: () => void;
};

export function UserFormModal({
    duration,
  submitError,
  isSubmitting,
  onChange,
  onSubmit,
  onClose,
}: UserFormModalProps) {
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
            Enter amount of days to suspend

            <input
              name="stock"
              type="number"
              value={duration}
              onChange={onChange}
              min="-50"
              step="1"
              disabled={isSubmitting}

            />

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