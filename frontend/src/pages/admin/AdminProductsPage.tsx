import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../lib/api";
import { money } from "../../lib/money";
import { BackToAdminButton } from "../../components/buttons/BackToAdminButton";
import { useState } from "react";
import type { AdminProductTab, Product, ProductStatus} from "../../types";

const tabs: AdminProductTab[] = ["ACTIVE", "INACTIVE", "ARCHIVED", "ALL"];

export function AdminProductsPage() {
  const [selectedTab, setSelectedTab] = useState<AdminProductTab>("ACTIVE");

  const queryClient = useQueryClient();

  const {
    data: products,
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: ["admin-products", selectedTab],
    queryFn: () => api.adminGetProducts(selectedTab),
  });

  const [newProduct, setNewProduct] = useState({
    name: "",
    price: "",
    stock: 0,
  });

  const [editingProductId, setEditingProductId] = useState<number | null>(null);

  const [editProduct, setEditProduct] = useState({
    name: "",
    price: "",
    stock: 0,
  });

  const addMutation = useMutation({
    mutationFn: api.adminAddProduct,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-products"] });
      queryClient.invalidateQueries({ queryKey: ["products"] });

      setNewProduct({
        name: "",
        price: "",
        stock: 0,
      });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({
      product,
    }: {
      product: {
        id: number;
        name: string;
        price: number;
        stock: number;
      };
    }) => api.adminUpdateProduct(product),

    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin-products"] });
      await queryClient.invalidateQueries({ queryKey: ["products"] });

      setEditingProductId(null);
    },
  });

  const statusMutation = useMutation({
    mutationFn: ({
      productId,
      status,
    }: {
      productId: number;
      status: ProductStatus;
    }) => api.changeProductStatus(productId, status),

    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-products"] });
      queryClient.invalidateQueries({ queryKey: ["products"] });
    },
  });

  const removeMutation = useMutation({
    mutationFn: (productId: number) => api.adminRemoveProduct(productId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin-products"] });
      await queryClient.invalidateQueries({ queryKey: ["products"] });
    },
  });

  function changeStatus(productId: number, status: ProductStatus) {
    statusMutation.mutate({ productId, status });
  }

  function startEdit(product: Product) {
    setEditingProductId(product.id);

    setEditProduct({
      name: product.name,
      price: String(product.price),
      stock: product.stock ?? 0,
    });
  }

  function cancelEdit() {
    setEditingProductId(null);

    setEditProduct({
      name: "",
      price: "",
      stock: 0,
    });
  }

  function handleChange(event: React.ChangeEvent<HTMLInputElement>) {
    const { name, value } = event.target;

    setNewProduct((current) => ({
      ...current,
      [name]: value,
    }));
  }

  function handleAddProduct() {
    addMutation.mutate({
      name: newProduct.name,
      price: Number(newProduct.price),
      stock: Number(newProduct.stock),
    });
  }

  function handleEditChange(event: React.ChangeEvent<HTMLInputElement>) {
    const { name, value } = event.target;

    setEditProduct((current) => ({
      ...current,
      [name]: value,
    }));
  }

  function handleSaveEdit(productId: number) {
    updateMutation.mutate({
      product: {
        id: productId,
        name: editProduct.name,
        price: Number(editProduct.price),
        stock: Number(editProduct.stock),
      },
    });
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
              const isEditing = editingProductId === product.id;

              return (
                <tr key={product.id}>
                  <td>{product.id}</td>

                  <td className="admin-edit">
                    {isEditing ? (
                      <input
                        name="name"
                        value={editProduct.name}
                        onChange={handleEditChange}
                      />
                    ) : (
                      product.name
                    )}
                  </td>

                  <td className="admin-edit">
                    {isEditing ? (
                      <input
                        name="price"
                        type="number"
                        value={editProduct.price}
                        onChange={handleEditChange}
                      />
                    ) : (
                      money(product.price)
                    )}
                  </td>

                  <td className="admin-edit">
                    {isEditing ? (
                      <input
                        name="stock"
                        type="number"
                        value={editProduct.stock}
                        onChange={handleEditChange}
                      />
                    ) : (
                      product.stock ?? 0
                    )}
                  </td>

                  <td className="admin-edit">
                    <select
                      value={product.status}
                      disabled={statusMutation.isPending}
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
                    {isEditing ? (
                      <div className="table-actions">
                        <button
                          className="button"
                          onClick={() => handleSaveEdit(product.id)}
                          disabled={updateMutation.isPending}
                        >
                          {updateMutation.isPending ? "Saving..." : "Save"}
                        </button>

                        <button className="button ghost" onClick={cancelEdit}>
                          Cancel
                        </button>
                      </div>
                    ) : (
                      <div className="table-actions">
                        <button
                          className="button icon-button"
                          onClick={() => startEdit(product)}
                        >
                          <img src="/icons/edit.png" />
                        </button>

                        <button
                          className="button danger"
                          onClick={() => removeMutation.mutate(product.id)}
                          disabled={removeMutation.isPending}
                        >
                          X
                        </button>
                      </div>
                    )}
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
                  onChange={handleChange}
                  placeholder="Product name"
                />
              </td>

              <td>
                <input
                  name="price"
                  type="number"
                  value={newProduct.price}
                  onChange={handleChange}
                  placeholder="Price"
                />
              </td>

              <td>
                <input
                  name="stock"
                  type="number"
                  value={newProduct.stock}
                  onChange={handleChange}
                  placeholder="Stock"
                />
              </td>

              <td>Active</td>

              <td>
                <button
                  className="button"
                  onClick={handleAddProduct}
                  disabled={addMutation.isPending}
                >
                  {addMutation.isPending ? "Adding..." : "Add"}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

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