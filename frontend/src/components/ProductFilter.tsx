import { ChangeEvent, useEffect, useRef, useState } from "react"
import { Slider } from "./Slider";
import { PriceRange } from "../pages/ProductsPage";


type ProductFilterProps = {
  maxAllowed: number;
  priceRange: PriceRange;
  filters: Record<string, Set<string>>;
  onPriceChange: (range: PriceRange) => void;
};

export function ProductFilter({filters, maxAllowed, priceRange,onPriceChange}:ProductFilterProps) {
  const [isOpen, setIsOpen] = useState([]);


  return (
    <div className="product-filter">
      <div className="filter-price">
        <h3>Price</h3>        
        <Slider maxAllowed={maxAllowed} priceRange={priceRange} onPriceChange={onPriceChange}></Slider>
      </div>
      <div>
        { Object.entries(filters).map(([key,values])=>(
          <div key={key}>
            <details>
              <summary>{key}</summary>
              {[...values].map((value)=>(
                <label>
                  <input
                  type="checkbox">
                </input> {value}</label>
                
              ))}
            </details>
          </div>
        ))

        }
          
        

      </div>

    </div>
  )
}