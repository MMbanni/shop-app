import { money } from "../../lib/money";
import { BackToAdminButton } from "../../components/buttons/BackToAdminButton";
import { useState, useRef } from "react";
import type { AdminProductTab, Product, ProductFormErrors, ProductStatus } from "../../types";
import { ApiErrorMessage } from "../../components/messages/ApiErrorMessage";
import { useAdminProducts } from "../../hooks/useAdminProductActions";
import { ProductForm } from "../../types/product";
import { getFieldErrors, getFormErrorMessage } from "../../lib/ApiError";
import { ProductFormModal } from "../../components/admin/ProductFormModal";
import { FloatingMessage } from "../../components/messages/FloatingMessage";

const tabs: AdminProductTab[] = ["ACTIVE", "INACTIVE", "ARCHIVED", "ALL"];

const emptyProductForm: ProductForm = {
  name: "",
  price: "",
  stock: "0",
};


export function AdminProductsPage() {
  const [selectedTab, setSelectedTab] = useState<AdminProductTab>("ACTIVE");

  const [message, setMessage] = useState<string | null>(null);
  const [messageVisible, setMessageVisible] = useState<boolean>(false);

  const addButtonRef = useRef<HTMLButtonElement>(null);
  const [messageAnchor, setMessageAnchor] = useState<HTMLButtonElement | null>(null);

  const [localAddErrors, setLocalAddErrors] = useState<ProductFormErrors>({});
  const [localUpdateErrors, setLocalUpdateErrors] = useState<ProductFormErrors>({});

  function showCartMessage() {
    setMessageVisible(true)

    setTimeout(() => {
      setMessageVisible(false)

    }, 3000);

  }

  const {
    adminProductsQuery,
    addProduct,
    updateProduct,
    changeProductStatus,
    removeProduct
  } = useAdminProducts(selectedTab);

  const editFieldErrors = {
    ...(updateProduct.isError ? getFieldErrors(updateProduct.error) : {}),
    ...localUpdateErrors
  };

  const editSubmitError = updateProduct.isError
    ? getFormErrorMessage(updateProduct.error)
    : null;

  const addFieldErrors = {
    ...(addProduct.isError ? getFieldErrors(addProduct.error) : {}),
    ...localAddErrors,
  };

  const addSubmitError = addProduct.isError
    ? getFormErrorMessage(addProduct.error)
    : null;


  const {
    data: products,
    isLoading,
    isError,
    error,
  } = adminProductsQuery;

  const [newProduct, setNewProduct] = useState<ProductForm>(emptyProductForm);

  const [editingProductId, setEditingProductId] = useState<number | null>(null);

  const [editingVersion, setEditingVersion] = useState<number | null>(null);

  const [editProduct, setEditProduct] = useState<ProductForm>(emptyProductForm);

  function handleAddProduct() {


    const price = Number(newProduct.price);
    const stock = Number(newProduct.stock);
    const errors: ProductFormErrors = {};

    if ( newProduct.price.trim() === "" || !Number.isFinite(price)) {
      errors.price = "Price must be a valid number"
    }
    if ( newProduct.stock.trim() === "" ||!Number.isInteger(stock)) {
      errors.stock = "Stock must be a valid number"
    }

    setLocalAddErrors(errors);
    if (Object.keys(errors).length > 0) return;


    addProduct.mutate({
      name: newProduct.name,
      price,
      stock
    },
      {
        onSuccess: () => {
          setNewProduct(emptyProductForm);
          setMessageAnchor(addButtonRef.current);
          setMessage(`${newProduct.name} added to inactive products`);
          showCartMessage()
        },
      }
    );
  }

  function startEdit(product: Product) {
    setLocalUpdateErrors({});
    updateProduct.reset();

    setEditingProductId(product.id);
    setEditingVersion(product.version);

    setEditProduct({
      name: product.name,
      price: String(product.price),
      stock: String(product.stock),
    });   

  }

  function cancelEdit() {
    setLocalUpdateErrors({});
    setEditingProductId(null);
    setEditProduct(emptyProductForm);
  }


  function changeStatus(productId: number, status: ProductStatus) {
    removeProduct.reset()
    changeProductStatus.mutate({ productId, status });
  }

  function handleRemoveProduct(productId: number) {
    changeProductStatus.reset();

    removeProduct.mutate(productId);
  }

  function handleAddProductChange(event: React.ChangeEvent<HTMLInputElement>) {
    if (addProduct.isPending) return;

    const { name, value } = event.target;

    setNewProduct((current) => ({
      ...current,
      [name]: value,
    }));

    setLocalAddErrors(prev => ({
      ...prev,
      [name]: undefined
    }));

    addProduct.reset();
  }


  function handleEditProductChange(event: React.ChangeEvent<HTMLInputElement>) {
    const { name, value } = event.target;

    setEditProduct((current) => ({
      ...current,
      [name]: value,
    }));

    setLocalUpdateErrors(prev => ({
      ...prev,
      [name]: undefined
    }));

    updateProduct.reset();
  }

  function handleSaveEdit(productId: number) {
    if (editingVersion === null) return;

    const price = Number(editProduct.price);
    const stock = Number(editProduct.stock);
    const errors: ProductFormErrors = {};

    if ( editProduct.price.trim() === "" || !Number.isFinite(price)) {
      errors.price = "Price must be a valid number"
    }
    if ( editProduct.stock.trim() === "" ||!Number.isInteger(stock)) {
      errors.stock = "Stock must be a valid number"
    }
    
    setLocalUpdateErrors(errors);
    if (Object.keys(errors).length > 0) return;


    updateProduct.mutate({
      id: productId,
      expectedVersion: editingVersion,
      name: editProduct.name,
      price,
      stock
    },
      {
        onSuccess: () => {
          setEditingProductId(null);
          setEditProduct(emptyProductForm);
        }
      });
  }

  function handleAddProductErrors(field: keyof ProductForm) {
    const message = addFieldErrors[field];

    if (!message) {
      return null;
    }

    return <div className="add-product-error" role="alert">
      {message}
    </div>

  }

  if (isLoading) {
    return <p className="page-message">Loading admin products...</p>;
  }

  if (isError) {
    return (
      <p className="page-message error">
        {error instanceof Error ? error.message : "Could not load products."}
      </p>
    );
  }

  const sortedProducts = [...(products ?? [])].sort(
    (a, b) => a.id - b.id
  );

  return (
    <main className="page-shell narrow">
      <div className="page-heading">
        <p className="section-label">Admin</p>
        <h1>Products</h1>
        <p className="muted"></p>
      </div>

      <div className="tabs">
        {tabs.map((tab) => (
          <button
            key={tab}
            className={selectedTab === tab ? "tab active" : "tab"}
            onClick={() => setSelectedTab(tab)}
          >
            {formatTab(tab)}
          </button>
        ))}
      </div>

      {removeProduct.isError && (
        <ApiErrorMessage
          error={removeProduct.error}
          fallback="Could not delete the product."
        />
      )}

      {changeProductStatus.isError && (
        <ApiErrorMessage
          error={changeProductStatus.error}
          fallback="Could not change the product status."
        />
      )}
      <div className="table-card">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Price</th>
              <th>Stock</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>

          <tbody>
            {sortedProducts?.map((product) => {
              const isSelected = editingProductId === product.id;

              return (
                <tr key={product.id}
                  className={isSelected ? "selected-product-row" : ""}
                >

                  <td>{product.id}</td>
                  <td>{product.name}</td>
                  <td>{money(product.price)}</td>
                  <td>{product.stock ?? 0}</td>



                  <td>
                    <select
                      value={product.status}
                      disabled={changeProductStatus.isPending || removeProduct.isPending}
                      onChange={(event) =>
                        changeStatus(
                          product.id,
                          event.target.value as ProductStatus
                        )
                      }
                    >
                      <option value="ACTIVE">Active</option>
                      <option value="INACTIVE">Inactive</option>
                      <option value="ARCHIVED">Archived</option>
                    </select>
                  </td>

                  <td>

                    <div className="table-actions">
                      <button
                        className="button icon-button"
                        onClick={() => startEdit(product)}
                        aria-label={`Edit ${product.name}`}
                      >
                        <img src="/icons/edit.png" />
                      </button>

                      <button
                        className="button danger"
                        onClick={() => handleRemoveProduct(product.id)}
                        disabled={removeProduct.isPending || changeProductStatus.isPending}
                      >
                        ×
                      </button>
                    </div>

                  </td>
                </tr>
              );
            })}

            <tr className="admin-add">
              <td>
                <div className="admin-add-control">New</div>
              </td>

              <td>
                <div className="admin-add-field">
                  <input
                    name="name"
                    value={newProduct.name}
                    disabled={addProduct.isPending}
                    onChange={handleAddProductChange}
                    placeholder="Product name"
                  />
                  <div className="admin-add-error-slot">
                    {handleAddProductErrors("name")}
                  </div>
                </div>
              </td>

              <td>
                <div className="admin-add-field">
                  <input
                    name="price"
                    type="number"
                    value={newProduct.price}
                    disabled={addProduct.isPending}
                    onChange={handleAddProductChange}
                    placeholder="Price"
                  />
                  <div className="admin-add-error-slot">
                    {handleAddProductErrors("price")}
                  </div>
                </div>
              </td>

              <td>
                <div className="admin-add-field">
                  <input
                    name="stock"
                    type="number"
                    value={newProduct.stock}
                    disabled={addProduct.isPending}
                    onChange={handleAddProductChange}
                    placeholder="Stock"
                  />
                  <div className="admin-add-error-slot">
                    {handleAddProductErrors("stock")}
                  </div>
                </div>
              </td>

              <td>
                <div className="admin-add-control">Inactive</div>
              </td>

              <td>
                <div className="admin-add-control">
                  <button
                    ref={addButtonRef}
                    className="button"
                    onClick={handleAddProduct}
                    disabled={addProduct.isPending || editingProductId != null}
                  >
                    {addProduct.isPending ? "Adding..." : "Add"}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        {addSubmitError && (
          <p className="error" role="alert">
            {addSubmitError}
          </p>
        )}

      </div>

      {editingProductId !== null && (
        <ProductFormModal
          title="Edit Product"
          form={editProduct}
          errors={editFieldErrors}
          submitError={editSubmitError}
          isSubmitting={updateProduct.isPending}
          onChange={handleEditProductChange}
          onSubmit={() => handleSaveEdit(editingProductId)}
          onClose={cancelEdit}
        />
      )}

      <FloatingMessage className="addedToCartMessage" anchor={messageAnchor} message={message ? message : ""} visible={messageVisible} ></FloatingMessage>


      <BackToAdminButton />
    </main>
  );
}

function formatTab(tab: AdminProductTab) {
  if (tab === "ALL") {
    return "All";
  }

  return tab.charAt(0) + tab.slice(1).toLowerCase();
}