import { useState, type ChangeEvent } from "react";
import type { ProductForm, ProductFormErrors } from "../../types";
import "./FormModal.css";

type ProductFormModalProps = {
  title: string;
  form: ProductForm;
  errors: ProductFormErrors;
  submitError?: string | null
  isSubmitting: boolean;
  onChange: (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => void;
  onAttributeChange: (attributes: Record<string, string>) => void;
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
  onAttributeChange,
  onSubmit,
  onClose,
}: ProductFormModalProps) {

  const [addAttributeName, setAddAttributeName] = useState("");
  const [addAttributeValue, setAddAttributeValue] = useState("");
  const [attributeError, setAttributeError] = useState<string | null>(null);

  const [editingKey, setEditingKey] = useState("");
  const [editAttributeName, setEditAttributeName] = useState("");
  const [editAttributeValue, setEditAttributeValue] = useState("");

  const isEditing = Boolean(editingKey);

   function addAttribute() {
    const name = addAttributeName.trim();
    const value = addAttributeValue.trim();
    if (Object.keys(form.attributes).includes(name)) {
      setAttributeError("Attribute already exists");
      return;
    }

    if (!name || !value) {
      setAttributeError("Enter both an attribute name and a value.");
      return;
    }

    onAttributeChange({
      ...form.attributes,
      [name]: value
    })

    setAddAttributeName("");
    setAddAttributeValue("");
    setAttributeError(null);
  }

  function startEdit(key: string, value: string) {
    setEditingKey(key);
    setEditAttributeName(key)
    setEditAttributeValue(value);
    setAttributeError(null);
  }

  function cancelEdit() {
    setEditingKey("");
    setEditAttributeName("");
    setEditAttributeValue("");
    setAttributeError(null);
  }

  function editAttribute() {
    const name = editAttributeName.trim();
    const value = editAttributeValue.trim();
    const attributes = { ...form.attributes };

    if (Object.keys(form.attributes).includes(name) && name != editingKey) {
      setAttributeError("Attribute already exists");
      return;
    }

    delete attributes[editingKey]

    if (!name || !value) {
      setAttributeError("Enter both an attribute name and a value.");
      return;
    }

    onAttributeChange({
      ...attributes,
      [name]: value
    })

    setEditAttributeName("");
    setEditAttributeValue("");
    setEditingKey("");
    setAttributeError(null);
  }


  function removeAttribute(key: string) {
    const attributes = { ...form.attributes };
    delete attributes[key];

    onAttributeChange(attributes)
    setAttributeError(null);
  }

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (isSubmitting || isEditing) return;

    if (isEditing) {
      setAttributeError("Confirm or cancel the attribute edit first.");
      return;
    }

    if (addAttributeName.trim() || addAttributeValue.trim()) {
      setAttributeError("Click Add or clear the new attribute fields first.");
      return;
    }
    
    onSubmit();
  }

  return (
    <div
      className="modal-backdrop"
      onPointerDown={(event) => {
        if (event.target === event.currentTarget && !isSubmitting) {
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
          <h2 id="modal-title">{title}</h2>

          <button
            type="button"
            className="modal-close"
            onClick={onClose}
            disabled={isSubmitting}
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

          <div className="product-details">
            <h3>Product details</h3>

            <label>
              Product name

              <input
                name="name"
                value={form.name}
                onChange={onChange}
                disabled={isSubmitting}
                aria-invalid={Boolean(errors.name)}
                aria-describedby={
                  errors.name ? "name-error" : undefined
                }
              />

              {errors.name && (
                <span
                  id="name-error"
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
                  errors.price ? "price-error" : undefined
                }
              />

              {errors.price && (
                <span
                  id="price-error"
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
                  errors.stock ? "stock-error" : undefined
                }
              />

              {errors.stock && (
                <span
                  id="stock-error"
                  className="field-error"
                  role="alert"
                >
                  {errors.stock}
                </span>
              )}
            </label>

            <label>
              Description

              <textarea
                name="description"
                maxLength={500}
                value={form.description}
                onChange={onChange}
                disabled={isSubmitting}
                aria-invalid={Boolean(errors.description)}
                aria-describedby={
                  errors.description ? "description-error" : undefined
                }
              />

              {errors.description && (
                <span
                  id="description-error"
                  className="field-error"
                  role="alert"
                >
                  {errors.description}
                </span>
              )}
            </label>

            <label>
              Image URL

              <input
                name="imageUrl"
                step="1"
                value={form.imageUrl}
                onChange={onChange}
                disabled={isSubmitting}
                aria-invalid={Boolean(errors.imageUrl)}
                aria-describedby={
                  errors.imageUrl ? "imageUrl-error" : undefined
                }
              />

              {errors.imageUrl && (
                <span
                  id="imageUrl-error"
                  className="field-error"
                  role="alert"
                >
                  {errors.imageUrl}
                </span>
              )}
            </label>
          </div>

          <div className="product-attributes">
            <h3>Product attributes</h3>

            <table className="attribute-table" >
              <thead>
                <tr>
                  <th>Attribute</th>
                  <th>Value</th>
                  <th> Actions </th>
                </tr>

              </thead>

              <tbody>
                {Object.entries(form.attributes).map(([key, value]) =>
                  <tr key={key}>
                    {editingKey === key ?
                      <>
                        <td><input
                          name="name"
                          value={editAttributeName}
                          onChange={(event) => setEditAttributeName(event.target.value)}
                          aria-invalid={Boolean(attributeError || errors.attributes)}
                          aria-describedby={
                            attributeError || errors.attributes ? "attribute-error" : undefined
                          }
                          
                        />
                        </td>
                        <td><input
                          name="value"
                          value={editAttributeValue}
                          onChange={(event) => setEditAttributeValue(event.target.value)}
                          aria-invalid={Boolean(attributeError || errors.attributes)}
                          aria-describedby={
                            attributeError || errors.attributes ? "attribute-error" : undefined
                          }
                        />
                        </td>
                        <td>
                          <div className="table-actions">
                            <button
                              type="button"
                              className="button icon-button"
                              onClick={() => editAttribute()}
                              aria-label={`Edit ${key}`}
                              disabled={isSubmitting}
                              
                            >
                              ✓
                            </button>

                            <button
                              type="button"

                              className="button danger"
                              onClick={() => cancelEdit()}
                              disabled={isSubmitting}

                            >
                              ×
                            </button>
                          </div>
                        </td>
                      </>
                      :
                      <><td>{key}</td>
                        <td>{value}</td>
                        <td>
                          <div className="table-actions">
                            <button
                              type="button"
                              className="button icon-button"
                              disabled={isEditing || isSubmitting}
                              onClick={() => startEdit(key, value)}
                              aria-label={`Edit ${key}`}
                            >
                              Edit
                            </button>

                            <button
                              type="button"
                              disabled={isEditing || isSubmitting}
                              className="button danger"
                              onClick={() => removeAttribute(key)}

                            >
                              -
                            </button>
                          </div>
                        </td>
                      </>
                    }


                  </tr>
                )}

                <tr>
                  <td>
                    <input
                      value={addAttributeName}
                      onChange={(event) => setAddAttributeName(event.target.value)}
                      disabled={isSubmitting}
                      aria-invalid={Boolean(attributeError || errors.attributes)}
                      aria-describedby={
                        attributeError || errors.attributes ? "attribute-error" : undefined
                      }
                    />
                  </td>
                  <td>
                    <input
                      value={addAttributeValue}
                      onChange={(event) => setAddAttributeValue(event.target.value)}
                      disabled={isSubmitting}
                      aria-invalid={Boolean(attributeError || errors.attributes)}
                      aria-describedby={
                        attributeError || errors.attributes ? "attribute-error" : undefined
                      }></input>
                  </td>
                  <td>
                    <button
                      type="button"
                      className="button"
                      onClick={addAttribute}> Add</button>
                  </td>
                </tr>
              </tbody>


            </table>

            {(attributeError || errors.attributes) && (
              <p id="attribute-error" className="error" role="alert">
                {attributeError || errors.attributes}
              </p>
            )}
          </div>

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