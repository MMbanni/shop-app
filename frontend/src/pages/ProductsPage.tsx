import { useQuery } from "@tanstack/react-query";
import { ProductCard } from "../components/ProductCard";
import { api } from "../lib/api";
import { useState } from "react";
import { FloatingMessage } from "../components/messages/FloatingMessage";

export function ProductsPage() {
  const { data: products, isLoading, isError, error } = useQuery({
    queryKey: ["products"],
    queryFn: api.products
  });

  const [cartMessage, setCartMessage] = useState<string|null>(null);
  const [cartMessageVisible, setCartMessageVisible] = useState<boolean>(false);


  function showCartMessage(message: string){
    setCartMessage(message);
    setCartMessageVisible(true)


    setTimeout(()=>{
    setCartMessageVisible(false)
      
    }, 3000);

  }


  if (isLoading) {
    return <p className="page-message">Loading products...</p>;
  }

  if (isError) {
    return <p className="page-message error">{error.message}</p>;
  }

  return (
    <main className="page-shell">
      <div className="page-heading">
        <p className="eyebrow">Products</p>
        <h1>Choose your product</h1>
        <p className="muted"></p>
      </div>

      <section className="product-grid">
        {products?.map((product) => 
        <ProductCard
         key={product.id} 
         product={product} 
         onAddSuccess={()=> showCartMessage(`${product.name} added to cart`)} 
         />)}
      </section>

      {cartMessage && <FloatingMessage message={cartMessage} visible={cartMessageVisible}></FloatingMessage>}
    </main>
  );
}
