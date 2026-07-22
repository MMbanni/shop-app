import { money } from "../../lib/money";
import { BackToAdminButton } from "../../components/buttons/BackToAdminButton";
import { useState } from "react";
import type { AdminProductTab, ApiErrorResponse, Product, ProductStatus } from "../../types";
import { useAdminProducts } from "../../hooks/useAdminProductActions";
import { ProductForm } from "../../types/product";
import { ApiError, getApiError } from "../../lib/ApiError";
import { ProductFormModal } from "../../components/admin/ProductFormModal";

const tabs: AdminProductTab[] = ["ACTIVE", "INACTIVE", "ARCHIVED", "ALL"];

const emptyProductForm: ProductForm = {
  name: "",
  price: "",
  stock: "0",
};

export function AdminProductsPage() {
  const [selectedTab, setSelectedTab] = useState<AdminProductTab>("ACTIVE");
  const [errorResponse, setErrorResponse] = useState<ApiErrorResponse | null>(null);

  const {
    adminProductsQuery,
    addProduct,
    updateProduct,
    changeProductStatus,
    removeProduct } = useAdminProducts(selectedTab);


  const {
    data: products,
    isLoading,
    isError,
    error,
  } = adminProductsQuery;

  const [newProduct, setNewProduct] = useState<ProductForm>(emptyProductForm);

  const [editingProductId, setEditingProductId] = useState<number | null>(null);

  const [editProduct, setEditProduct] = useState<ProductForm>(emptyProductForm);

  function handleAddProduct() {
    addProduct.mutate({
      name: newProduct.name,
      price: Number(newProduct.price),
      stock: Number(newProduct.stock),
    },
      {
        onSuccess: () => setNewProduct(emptyProductForm),
        onError: (error) => {
          const newError = getApiError(error);
          if (newError) setErrorResponse(newError)
        }
      }

    );
  }

  function startEdit(product: Product) {
    setEditingProductId(product.id);
    setErrorResponse(null);

    setEditProduct({
      name: product.name,
      price: String(product.price),
      stock: String(product.stock ?? 0),
    });

  }

  function cancelEdit() {
    setEditingProductId(null);
    setEditProduct(emptyProductForm);
    setErrorResponse(null);
  }


  function changeStatus(productId: number, status: ProductStatus) {
    changeProductStatus.mutate({ productId, status });
  }

  function handleAddProductChange(event: React.ChangeEvent<HTMLInputElement>) {
    const { name, value } = event.target;

    setNewProduct((current) => ({
      ...current,
      [name]: value,
    }));
  }


  function handleEditProductChange(event: React.ChangeEvent<HTMLInputElement>) {
    const { name, value } = event.target;

    setEditProduct((current) => ({
      ...current,
      [name]: value,
    }));
  }

  function handleSaveEdit(productId: number) {
    updateProduct.mutate({
      id: productId,
      name: editProduct.name,
      price: Number(editProduct.price),
      stock: Number(editProduct.stock),
    },
      {
        onSuccess: () => {
          setEditingProductId(null);
          setEditProduct(emptyProductForm);
          setErrorResponse(null);
        },
        onError: (error) => {
          const newError = getApiError(error);
          if (newError) setErrorResponse(newError)
        }
      });
  }

  function handleAddProductErrors(name: string) {
    let fieldErrors = errorResponse?.errors

    if (fieldErrors) {
      for (const error of fieldErrors) {
        if (!editingProductId && error[name]) {
          return <p className="error" role="alert">
            {error[name]}

          </p>
        }

      }
    }

  }

  function getUpdateProductError(fieldName: keyof ProductForm): string | undefined {
    const fieldErrors = errorResponse?.errors

    if (!fieldErrors) return undefined;


    for (const fieldError of fieldErrors) {
      const belongsToField = Object.values(fieldError).includes(fieldName);
      const message = fieldError["message"];


      if (belongsToField && message) {
        return String(message)


      }

    }
    return undefined;

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
        <p className="eyebrow">Admin</p>
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
                      disabled={changeProductStatus.isPending}
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
                        onClick={() => removeProduct.mutate(product.id)}
                        disabled={removeProduct.isPending}
                      >
                        ×
                      </button>
                    </div>

                  </td>
                </tr>
              );
            })}

            <tr className="admin-add">
              <td>New</td>

              <td>
                <input
                  name="name"
                  value={newProduct.name}
                  onChange={handleAddProductChange}
                  placeholder="Product name"
                />
                {handleAddProductErrors("name")}

              </td>

              <td>
                <input
                  name="price"
                  type="number"
                  value={newProduct.price}
                  onChange={handleAddProductChange}
                  placeholder="Price"
                />
                {handleAddProductErrors("price")}


              </td>

              <td>
                <input
                  name="stock"
                  type="number"
                  value={newProduct.stock}
                  onChange={handleAddProductChange}
                  placeholder="Stock"
                />
                {handleAddProductErrors("stock")}

              </td>

              <td>Active</td>

              <td>
                <button
                  className="button"
                  onClick={handleAddProduct}
                  disabled={addProduct.isPending || editingProductId != null}
                >
                  {addProduct.isPending ? "Adding..." : "Add"}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      {editingProductId !== null && (
        <ProductFormModal
          title="Edit Product"
          form={editProduct}
          errors={{
            name: getUpdateProductError("name"),
            price: getUpdateProductError("price"),
            stock: getUpdateProductError("stock"),
          }}
          isSubmitting={updateProduct.isPending}
          onChange={handleEditProductChange}
          onSubmit={() => handleSaveEdit(editingProductId)}
          onClose={cancelEdit}
        />
      )}

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