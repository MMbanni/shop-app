import { ChangeEvent, useEffect, useRef, useState } from "react"
import { Slider } from "./Slider";
import { PriceRange } from "../pages/ProductsPage";


type ProductFilterProps = {
  priceRange: PriceRange;
  onPriceChange: (range: PriceRange) => void;
};

export function ProductFilter({priceRange,onPriceChange}:ProductFilterProps) {

  return (
    <div className="product-filter">
      <div className="filter-price">
        <h3>Price</h3>
        
        <Slider priceRange={priceRange} onPriceChange={onPriceChange}></Slider>
      </div>

    </div>
  )
}