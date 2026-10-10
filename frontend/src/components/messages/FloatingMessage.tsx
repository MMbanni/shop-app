import { useLayoutEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";

type BannerProps = {
  className: string
  message: string;
  anchor: HTMLElement | null;
  visible: boolean;
};

export function FloatingMessage({ 
  className="",
  message,
  visible, 
  anchor
}: BannerProps) {

  const messageRef = useRef<HTMLParagraphElement>(null);

  const [position, setPosition] = useState({
    top: 0,
    left: 0,
  });

  useLayoutEffect(() => {
    if (!anchor || !visible) {
      return;
    }

  function updatePosition() {
      if (!anchor) {
        return;
      }

      // Position of the clicked Add button.
      const buttonRect = anchor.getBoundingClientRect();

      const messageWidth =
        messageRef.current?.offsetWidth ?? 0;

      // Centre the message under the button.
      const desiredLeft =
        buttonRect.left + buttonRect.width / 2;

      const pagePadding = 16;
      const halfMessageWidth = messageWidth / 2;

      // Prevent the message from leaving the screen.
      const safeLeft = Math.min(
        Math.max(
          desiredLeft,
          pagePadding + halfMessageWidth,
        ),
        window.innerWidth -
          pagePadding -
          halfMessageWidth,
      );

      setPosition({
        top: buttonRect.bottom + 8,
        left: safeLeft,
      });
    }
    updatePosition();

    // Keep it under the button if the page moves.
    window.addEventListener("resize", updatePosition);
    window.addEventListener(
      "scroll",
      updatePosition,
      true,
    );

    return () => {
      window.removeEventListener(
        "resize",
        updatePosition,
      );

      window.removeEventListener(
        "scroll",
        updatePosition,
        true,
      );
    };
  }, [anchor, message, visible]);

    

  if (
    !anchor ||
    !message ||
    typeof document === "undefined"
  ) {
    return null;
  }

  return createPortal(
    <p
      ref={messageRef}
      role="status"
      aria-live="polite"
      aria-atomic="true"
      className={`${className} floating-message ${
        visible ? "show" : "hide"
      }`}
      style={{
        top: position.top,
        left: position.left,
      }}
    >
      {message}
    </p>,
    document.body,
  );
}