import { useQuery } from "@tanstack/react-query";
import { ProductCard } from "../components/ProductCard";
import { api } from "../lib/api";
import { useEffect, useRef, useState } from "react";
import { FloatingMessage } from "../components/messages/FloatingMessage";
import { ProductFilter } from "../components/ProductFilter";

type CartMessage = {
  text: string;
  anchor: HTMLButtonElement;
};

export function ProductsPage() {
  const { data: products, isLoading, isError, error } = useQuery({
    queryKey: ["products"],
    queryFn: api.products
  });

  const [cartMessage, setCartMessage] = useState<CartMessage | null>(null);
  const [cartMessageVisible, setCartMessageVisible] = useState<boolean>(false);


  const messageTimer = useRef<number | undefined>(undefined);

  useEffect(() => {
    return () => window.clearTimeout(messageTimer.current);
  }, []);

  function showCartMessage(text: string, button: HTMLButtonElement) {
    window.clearTimeout(messageTimer.current);

    setCartMessage({ text, anchor: button });
    setCartMessageVisible(true);

    messageTimer.current = window.setTimeout(() => {
      setCartMessageVisible(false);
    }, 3000);
  }


  if (isLoading) {
    return <p className="page-message">Loading products...</p>;
  }

  if (isError) {
    return (
      <p className="page-message error">
        {error instanceof Error
          ? error.message
          : "Could not load products"
        }

      </p>
    );
  }

  return (
    <main className="page-shell">
      <div className="page-heading">
        <h1>Products</h1>
      </div>
      <div className="products-container">

      <ProductFilter></ProductFilter>

      <section className="product-grid">
        {products?.map((product) =>
          <ProductCard
            key={product.id}
            product={product}
            onAddSuccess={(button) => showCartMessage(`${product.name} added to cart`, button)}

          />)}
      </section>
      <FloatingMessage
        className="added-to-cart-message"
        message={cartMessage?.text ?? ""}
        anchor={cartMessage?.anchor ?? null}
        visible={cartMessageVisible}
      />
      </div>

    </main>
  );
}
