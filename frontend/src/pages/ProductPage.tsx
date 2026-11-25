import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../lib/api";
import { useNavigate, useParams } from "react-router-dom";
import { ApiError, getApiError } from "../lib/ApiError";
import { money } from "../lib/money";
import { useState } from "react";
import { getErrorMessage } from "../lib/ApiError";
import { useAuth } from "../context/AuthContext";
import { Product } from "../types";
import "./ProductPage.css"

export function ProductPage() {
  const { id } = useParams();
  const query = useQuery({
    queryKey: ["products", id],
    queryFn: () => api.product(id!),
    enabled: Boolean(id),
  });
  if (!id) return <p className="page-message error">Missing product ID.</p>;
  if (query.isPending) return <p className="page-message" role="status">Loading product…</p>;
  if (query.isError) return <p className="page-message error" role="alert">{getErrorMessage(query.error)}</p>;
  if (!query.data) return <p className="page-message">Product not found.</p>;

  return <ProductDetails key={query.data.id} product={query.data} />;
}

function ProductDetails({product}:{product: Product}){
  const { isLoggedIn } = useAuth();

  const navigate = useNavigate();

  const queryClient = useQueryClient();

  const [selectedUrl, setSelectedUrl] = useState<string | null>(null);

  const images = (product.images ?? []).filter((url) => url.trim().length > 0);

  const activeUrl = selectedUrl && images.includes(selectedUrl) ? selectedUrl : images[0];

  const attributes = Object.entries(product.attributes ?? {});

  const isOutOfStock = product.stock < 1;
  
  const stockMessage =
    isOutOfStock ? "Out of stock"
      : product.stock > 10
        ? "In stock"
        : `Only ${product.stock} left`;


  const fallbackImageUrl = "https://images.unsplash.com/photo-1651761580601-4a77e280c80f?w=1000&h=800&auto=format&fit=crop";


  const addMutation = useMutation({
    mutationFn: (productId: number) => api.addToCart(productId, 1),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["cart"] }),
  });


  const addError = addMutation.isError
    ? getApiError(addMutation.error)?.detail ??
    "We couldn't add this product to your cart."
    : null;


  function handleAddToCart() {
    if (!product || isOutOfStock || addMutation.isPending) return;

    if (!isLoggedIn) {
      navigate("/login", {
        state: { returnTo: `/products/${product.id}` },
      });
      return;
    }

    addMutation.mutate(product.id);
  }


    return (
    <main className="page-shell shop-product-page">
      <div className="shop-product-layout">

        {/* LEFT — images */}
        <section className="shop-product-gallery">
          {images.length > 1 && (
            <div className="shop-product-thumbnails">
              {images.map((url, index) => (
                <button
                  key={`${url}-${index}`}
                  type="button"
                  aria-label={`View image ${index + 1}`}
                  aria-pressed={url === activeUrl}
                  onClick={() => setSelectedUrl(url)}
                >
                  <img src={url} alt="" />
                </button>
              ))}
            </div>
          )}

          <div className="shop-product-main-image">
            <img
              src={activeUrl || fallbackImageUrl}
              alt={product.name}
            />
          </div>
        </section>

        {/* MIDDLE — name and description */}
        <section className="shop-product-summary">
          <h1>{product.name}</h1>
          <p>{product.description}</p>
        </section>

        {/* RIGHT — purchase box */}
        <aside className="shop-product-actions">
          <strong className="shop-product-price">
            {money(product.price)}
          </strong>

          <p>{stockMessage}</p>

          <button
            type="button"
            className="button"
            onClick={handleAddToCart}
            disabled={isOutOfStock || addMutation.isPending}
          >
            {isOutOfStock
              ? "Out of stock"
              : addMutation.isPending
                ? "Adding..."
                : "Add to cart"}
          </button>

          <div aria-live="polite">
            {addError && <p className="error">{addError}</p>}

            {addMutation.isSuccess && (
              <p>Added to your cart.</p>
            )}
          </div>
        </aside>

        {/* BELOW — specifications */}
        {attributes.length > 0 && (
          <section className="shop-product-specs">
            <h2>Specifications</h2>

            <dl>
              {attributes.map(([name, value]) => (
                <div key={name}>
                  <dt>{name}</dt>
                  <dd>{value}</dd>
                </div>
              ))}
            </dl>
          </section>
        )}

      </div>
    </main>
  );

}