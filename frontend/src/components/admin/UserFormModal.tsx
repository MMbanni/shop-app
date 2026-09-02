import type { ChangeEvent } from "react";
import "./FormModal.css";


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
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
        aria-describedby={
          submitError ? "modal-submit-error" : undefined
        }
      >
        <header className="modal-header">
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
          className="modal-form"
          onSubmit={handleSubmit}
          noValidate>

          {submitError && (
            <div
              id="modal-submit-error"
              className="modal-error"
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

          <footer className="modal-actions">
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