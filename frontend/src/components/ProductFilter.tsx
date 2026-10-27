import { ChangeEvent, useEffect, useRef, useState } from "react"
import "./slider.css"

export function ProductFilter() {
  const [minPrice, setMinPrice] = useState(0);
  const [maxPrice, setMaxPrice] = useState(0);

  const sliderRef = useRef<HTMLDivElement>(null);
  
  useEffect(()=>{
    console.log(sliderRef.current?.getBoundingClientRect())
  },[])

  
  function changeMinPrice(event: ChangeEvent<HTMLInputElement>){
    setMinPrice(Number(event.target.value));
  }


  return (
    <div className="product-filter">
      <div className="filter-price">
        <h3>Price</h3>
        <p>{minPrice}</p>

        <div  ref={sliderRef} className="slider-track">
          <div 
          className="slider-min"
          onPointerDown={()=> console.log("min clicked")
          }></div>
          <div className="slider-max"></div>
        </div>
        <div className="slider-active"></div>
        

        <p>{maxPrice}</p> 
      </div>

    </div>
  )
}