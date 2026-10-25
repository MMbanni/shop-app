import { useState } from "react"

export function ProductFilter() {
  const [minPrice, setMinPrice] = useState(0);
  return (
    <div className="filter">
      <div className="filter-price">
        <h3>Price</h3>
          <input
            type="range"
            id="slider"
            min={0}
            max={50000}
            value={50000}
          />
            </div>

    </div>
  )
}