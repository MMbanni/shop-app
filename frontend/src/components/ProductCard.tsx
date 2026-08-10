import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "../lib/api";
import { money } from "../lib/money";
import type { Product } from "../types/product";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { FloatingMessage } from "../components/messages/FloatingMessage";
import { useState } from "react";
import { getApiError } from "../lib/ApiError";


type ProductCardProps = {
  product: Product;
  onAddSuccess: () => void
};

export function ProductCard({ product }: ProductCardProps) {
  const { isLoggedIn } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  
  const [message, setMessage] = useState<string|null>(null);

  const [messageVisible, setMessageVisible] = useState<boolean>(false);

  const isOutOfStock = product.stock < 1;
  const imageUrl = product.imageUrl || "https://images.unsplash.com/photo-1651761580601-4a77e280c80f?w=1000&h=800&auto=format&fit=crop";



  function showCartMessage() {
    setMessageVisible(true)

    setTimeout(() => {
      setMessageVisible(false)

    }, 3000);

  }

  const addMutation = useMutation({
    mutationFn: () => api.addToCart(product.id, 1),

    onSuccess: async () => {
      setMessage(`${product.name} added to cart`)
      showCartMessage();
      await queryClient.invalidateQueries({ queryKey: ["cart"] });
    }
  });

  function handleAddToCart() {
    if (!isLoggedIn) {
      navigate("/login");
      return;
    }

    addMutation.mutate();
  }

  const stockMessage = isOutOfStock ? "Out of stock"
    : product.stock > 10
      ? "In stock"
      : `Only ${product.stock} left`;

  const addError = addMutation.isError
    ? getApiError(addMutation.error)?.detail ??
    "We couldn't add this product to your cart."
    : null;


  return (
    <article className="product-card">
      <img src={imageUrl}
        alt={product.name}
      />

      <div className="product-body">
        <p className="eyebrow">{stockMessage}</p>

        <h3>{product.name}</h3>

        <p className="muted">{product.description}</p>

        <div className="product-footer">
          <strong>{money(product.price)}</strong>

          <button
            className="button"
            onClick={handleAddToCart}
            disabled={addMutation.isPending}
          >

            {isOutOfStock ?
              "Out of stock" :
              addMutation.isPending ? "Adding..." :
                "Add to cart"}
          </button>
        </div>
        
        <div className="product-error" aria-live="polite">
          {addError && (
            <p className="error">{addError}</p>
          )}
        </div>

        <FloatingMessage className="addedToCartMessage" message={message?message:""} visible={messageVisible} ></FloatingMessage>
      </div>
    </article>
  );
}