import { useQuery, useQueryClient } from "@tanstack/react-query";
import { ProductCard } from "../components/ProductCard";
import { api } from "../lib/api";
import { useEffect, useRef, useState } from "react";
import { FloatingMessage } from "../components/messages/FloatingMessage";
import { ProductFilter } from "../components/ProductFilter";
import { ButtonMessage } from "../types/components";
import "./ProductsPage.css"

export type PriceRange = {
  min: number;
  max: number;
};

export type Filters = Record<string, Set<string>>;
export type SelectedFilters = Record<string, string[]>;



export function ProductsPage() {
  const [priceQuery, setPriceQuery] = useState<PriceRange | null>(null);
  const [activeFilters, setActiveFilters] = useState<Filters>({});
  useEffect(()=> console.log(activeFilters), [activeFilters]
  )

  const { data: allProducts = [],
    isSuccess: catalogLoaded
  } = useQuery({
    queryKey: ["all-products"],
    queryFn: api.products,
  });

  const attributes = allProducts.flatMap((product) => Object.keys(product.attributes)); 
  const keys = [...new Set(attributes)];

  const filters: Filters = {}
  keys.forEach((key)=>{
    filters[key] = new Set
  })

  allProducts.forEach((product)=>{
    for(const key of keys){
      if(product.attributes[key]){
        filters[key].add(product.attributes[key])
      }
    }
  })


  const maxAllowed = Math.max(
    0,
    ...allProducts.map(product=>product.price)
  )

  const effectiveRange = priceQuery ?? {
    min:0,
    max: maxAllowed
  }

    useEffect(() => {
    return () => window.clearTimeout(messageTimer.current);
  }, []);
  
  const selectedAttributes = Object.fromEntries(
    Object.entries(activeFilters).map(
      ([key,value])=> [key,[...value]]
    )
  )

  const { data: products, isLoading, isError, error } = useQuery({
    queryKey: ["products", effectiveRange, selectedAttributes],
    queryFn: () => api.filterProducts(effectiveRange, selectedAttributes),
    enabled: catalogLoaded
  });


  const [cartMessage, setCartMessage] = useState<ButtonMessage | null>(null);
  const [cartMessageVisible, setCartMessageVisible] = useState<boolean>(false);


  const messageTimer = useRef<number | undefined>(undefined);

  useEffect(() => {
    return () => window.clearTimeout(messageTimer.current);
  }, []);

  function showCartMessage(message: ButtonMessage) {
    window.clearTimeout(messageTimer.current);

    setCartMessage(message);
    setCartMessageVisible(true);

    messageTimer.current = window.setTimeout(() => {
      setCartMessageVisible(false);
    }, 3000);
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
        
        { catalogLoaded &&
        <ProductFilter
          filters={filters} activeFilters={activeFilters} setActiveFilters={setActiveFilters}maxAllowed = {maxAllowed} priceRange={effectiveRange} onPriceChange={setPriceQuery}></ProductFilter>
        }
        { products && products?.length < 1 && <p>No products found, please widen your search</p>

        }

        <section className="product-grid">
          {isLoading && <p>Loading products...</p>}
          {products?.map((product) =>
            <ProductCard
              key={product.id}
              product={product}
              onAddSuccess={(button) => showCartMessage({text:`${product.name} added to cart`, anchor: button})}

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
