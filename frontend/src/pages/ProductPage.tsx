import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../lib/api";
import { useNavigate, useParams } from "react-router-dom";
import { ApiError, getApiError } from "../lib/ApiError";
import { money } from "../lib/money";
import { useRef } from "react";
import { getErrorMessage } from "../lib/ApiError";
import { useAuth } from "../context/AuthContext";

export function ProductPage() {
  const queryClient = useQueryClient();
  const { isLoggedIn } = useAuth();
  const navigate = useNavigate();

  const { id } = useParams();




  const { data: product, isLoading, isError, error } = useQuery({
    queryKey: ["products", id],
    queryFn: () => {
      if (!id) throw new Error("Missing id");
      return api.product(id)
    },
    enabled: Boolean(id)
  });

  const addMutation = useMutation({
    mutationFn: (productId: number) => api.addToCart(productId, 1),

    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["cart"] }),
  });


  if (!id) {
    return <p className="page-message error">Missing product ID.</p>;
  }

  if (isLoading) {
    return <p className="page-message">Loading product...</p>;
  }

  if (isError) {
    return (
      <p className="page-message error" role="alert">
        {getErrorMessage(error)}
      </p>
    );
  }

  if (!product) {
    return <p className="page-message">Product not found.</p>;
  }

  const isOutOfStock = product.stock < 1;

  const stockMessage =
    isOutOfStock ? "Out of stock"
      : product.stock > 10
        ? "In stock"
        : `Only ${product.stock} left`;


  const addError = addMutation.isError
    ? getApiError(addMutation.error)?.detail ??
    "We couldn't add this product to your cart."
    : null;


  function handleAddToCart() {
    if (!product || isOutOfStock || addMutation.isPending) return;

    if (!isLoggedIn) {
      navigate("/login", {
        state: { returnTo: `/products/${id}` },
      });
      return;
    }

    addMutation.mutate(product.id);
  }


  return (
    <main className="page-shell">
      <div className="product-container">
        <div className="page-heading">
          <h1>{product?.name}</h1>
        </div>
        <section className="photos">
          <img src={product?.imageUrl} alt="" />
        </section>
        <section className="product-description">
          {product?.description}
        </section>

        <section className="product-specifications">
  <h2>Specifications</h2>

  <dl>
    {Object.entries(product.attributes ?? {}).map(([name, value]) => (
      <div key={name}>
        <dt>{name}</dt>
        <dd>{value}</dd>
      </div>
    ))}
  </dl>
</section>

        <article className="add-box">
          
              <div className="product-footer">
                <strong className="price">{money(product.price)}</strong>                
              </div>

              <p className="section-label muted">{stockMessage}</p>

            
            <button

                  className="button"
                  onClick={handleAddToCart}
                  disabled={isOutOfStock || addMutation.isPending}
                >

                  {isOutOfStock ?
                    "Out of stock" :
                    addMutation.isPending ? "Adding..." :
                      "Add to cart"}
                </button>

                <div className="product-error" aria-live="polite">
                {addError && (
                  <p className="error">{addError}</p>
                )}
              </div>
            

            

          
        </article>


      </div>

    </main>
  )
}