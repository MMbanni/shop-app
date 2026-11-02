import { ChangeEvent, useEffect, useRef, useState } from "react"
import { Slider } from "./Slider";
import { Filters, PriceRange } from "../pages/ProductsPage";


type ProductFilterProps = {
  maxAllowed: number;
  priceRange: PriceRange;
  filters: Filters;
  activeFilters: Filters;
  onPriceChange: (range: PriceRange) => void;
  setActiveFilters: React.Dispatch<React.SetStateAction<Filters>>;

};

export function ProductFilter({ filters, activeFilters, setActiveFilters, maxAllowed, priceRange, onPriceChange }: ProductFilterProps) {

  function handleChecked(event: ChangeEvent<HTMLInputElement>, key: string, value: string) {
    const isChecked = event.target.checked;
    setActiveFilters(current => {
      const values = new Set(current[key] ?? []);

      isChecked ? values.add(value) : values.delete(value);

      return {
        ...current,
        [key]: values
      };
    })
  }


  return (
    <div className="product-filter">
      <div className="filter-price">
        <h3>Price</h3>
        <Slider maxAllowed={maxAllowed} priceRange={priceRange} onPriceChange={onPriceChange}></Slider>
      </div>
      <div>
        {Object.entries(filters).map(([key, values]) => (
          <details key={key}
            className="filter-section"
          >
            <summary className="filter-title">{key}</summary>
            <div className="filter-options">
              {[...values].map((value) => (
                <label key={value}>
                  <input
                    type="checkbox"
                    value={value}
                    checked={activeFilters[key]?.has(value) ?? false}
                    onChange={(event) => handleChecked(event, key, value)}>

                  </input> {value}</label>

              ))}
            </div>
          </details>

        ))

        }



      </div>

    </div>
  )
}