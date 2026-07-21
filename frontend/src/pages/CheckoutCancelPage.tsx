import { useMutation } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { api } from "../lib/api";
import { useEffect } from "react";

export function CheckoutCancelPage() {

  const cancelMutation = useMutation({
    mutationFn: api.cancelCheckout
  });

  useEffect(() => {
    cancelMutation.mutate();
  }, []);
  
  return (
    <main className="page-shell narrow">
      <div className="success-card">
        <p className="eyebrow">Checkout cancelled</p>
        <h1>No payment was taken.</h1>
        <p className="muted">You can go back to your cart and try again.</p>
        <Link className="button large" to="/cart">Back to cart</Link>
      </div>
    </main>
  );
}
