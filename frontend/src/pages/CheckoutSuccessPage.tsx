import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { api } from "../lib/api";
import { OrderStatus } from "../types";



export function CheckoutSuccessPage() {
  const [searchParams] = useSearchParams();
  const sessionId = searchParams.get("session_id");
  const [orderStatus, setOrderStatus] = useState<OrderStatus | null>(null);

  useEffect(() => {
    if (!sessionId) {
      return;
    }

    let cancelled = false;
    let timeoutId: number | undefined;

    async function checkOrderStatus() {
      const response = await api.orderStatus(sessionId!);

      if (cancelled) {
        return;
      }

      setOrderStatus(response.status);

      if (response.status === "PENDING") {
        timeoutId = window.setTimeout(
          checkOrderStatus,
          1500
        );
      }
    }

    void checkOrderStatus();

    return () => {
      cancelled = true;

      if (timeoutId !== undefined) {
        window.clearTimeout(timeoutId);
      }
    };
  }, [sessionId]);

  const paymentConfirmed = orderStatus === "PAID"

  return (
    <main className="page-shell narrow">
      <div className="success-card">
        <p className="section-label">{orderStatus ?? "CHECKING..."}</p>

        <h1>
          {paymentConfirmed
            ? "Thank you for your order."
            : "Confirming payment..."}
        </h1>

        {sessionId && <p className="tiny">Session: {sessionId}</p>}
        <Link className="button large" to="/products">Continue shopping</Link>
      </div>
    </main>
  );
}
