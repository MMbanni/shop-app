import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "../lib/api";
import { money } from "../lib/money";
import type { Product } from "../types/product";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

type ProductCardProps = {
  product: Product;
  onAddSuccess: () => void
};

export function ProductCard({ product, onAddSuccess }: ProductCardProps) {
  const { isLoggedIn } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const addMutation = useMutation({
    mutationFn: () => api.addToCart(product.id, 1),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["cart"] });
      onAddSuccess();
    }
  });

  function handleAddToCart() {
    if (!isLoggedIn) {
      navigate("/login");
      return;
    }

    addMutation.mutate();
  }

  const imageUrl = product.imageUrl || "https://images.unsplash.com/photo-1651761580601-4a77e280c80f?w=1000&h=800&auto=format&fit=crop";


  return (
    <article className="product-card">
      <img src={imageUrl} alt={product.name} />
      <div className="product-body">
        <p className="eyebrow">{
          product.stock < 1 ? "Out of stock" :
          product.stock > 10 ? "In stock" :
          `Only ${product.stock} left`
          }
        </p>
        
        <h3>{product.name}</h3>
        <p className="muted">{product.description}</p>
        <div className="product-footer">
          <strong>{money(product.price)}</strong>
          <button className="button" onClick={handleAddToCart} disabled={addMutation.isPending}>
            {addMutation.isPending ? "Adding..." : "Add to cart"}
          </button>
        </div>
        {addMutation.isError && <p className="error">{addMutation.error.message}</p>}
      </div>
    </article>
  );
}
