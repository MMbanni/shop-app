import React, { useRef, useState } from "react";
import "./slider.css"

export function Slider() {
  const sliderRef = useRef<HTMLDivElement>(null);
  const isDragging = useRef(false);

  const [minPrice, setMinPrice] = useState(0);
  const [maxPrice, setMaxPrice] = useState(50000);
  const minPosition = (minPrice/50000)*100;
  const maxPosition = (maxPrice/50000)*100;

  function fitTrack(value: number, min: number, max: number) {
    if (value > max) return max;
    if (value < min) return min;
    return value
  }

  function clampMin(value: number, max: number) {
    if (value >= max) return max;
    return value;
  }

  function clampMax(value: number, min: number) {
    if (value <= min) return min;
    return value;
  }

  function handlePointerDown(event: React.PointerEvent<HTMLDivElement>) {
    isDragging.current = true;

    event.currentTarget.setPointerCapture(event.pointerId);
  }

  function handlePointerUp(event: React.PointerEvent<HTMLDivElement>) {
    isDragging.current = false;
    event.currentTarget.releasePointerCapture(event.pointerId);
  }

  function handlePointerMove(event: React.PointerEvent<HTMLDivElement>) {
    if (!sliderRef.current) return;
    if (!isDragging.current) return;

    const rect = sliderRef.current.getBoundingClientRect();

    const pointerX = event.clientX - rect.left;

    const position = fitTrack(pointerX, 0, rect.width)
    const price =  (position/rect.width)*50000;

    if (event.currentTarget.className === "slider-min") {
      setMinPrice(Math.min(price,maxPrice));

    } else if (event.currentTarget.className === "slider-max") {
      setMaxPrice(Math.max(price,minPrice));

    }

  }

  return (
    <>
      <div ref={sliderRef} className="slider-track">
        <div
          className="slider-min"
          onPointerDown={(event) => handlePointerDown(event)}
          onPointerMove={(event) => handlePointerMove(event)}
          onPointerUp={(event) => handlePointerUp(event)}

          style={{ left: ` ${minPosition}%` }}
        ></div>

        <div className="slider-active"
        style={{
          left: `${minPosition}%`,
          width: `${maxPosition - minPosition}%`
        }}          
        >

        </div>

        <div className="slider-max"
          onPointerDown={(event) => handlePointerDown(event)}
          onPointerMove={(event) => handlePointerMove(event)}
          onPointerUp={(event) => handlePointerUp(event)}

          style={{ left: ` ${maxPosition}%` }}
        ></div>
        <p>{Math.round(minPrice)}</p>
        <p>{Math.round(maxPrice)}</p>

      </div>
    </>
  )

}