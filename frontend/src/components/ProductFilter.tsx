import { ChangeEvent, useEffect, useRef, useState } from "react"
import { Slider } from "./Slider";

export function ProductFilter() {
  const [minPrice, setMinPrice] = useState(0);
  const [maxPrice, setMaxPrice] = useState(0);


  
  function changeMinPrice(event: ChangeEvent<HTMLInputElement>){
    setMinPrice(Number(event.target.value));
  }


  return (
    <div className="product-filter">
      <div className="filter-price">
        <h3>Price</h3>
        
        <Slider></Slider>
      </div>

    </div>
  )
}