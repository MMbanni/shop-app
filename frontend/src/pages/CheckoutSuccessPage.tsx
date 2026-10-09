import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { api } from "../lib/api";
import { ApiErrorResponse, Order, OrderStatus } from "../types";
import { getErrorMessage, getApiError } from "../lib/ApiError";



export function CheckoutSuccessPage() {
  const [searchParams] = useSearchParams();
  const sessionId = searchParams.get("session_id");

  const [orderInfo, setOrderInfo] = useState<Order | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [retryCount, setRetryCount] = useState(0);


  useEffect(() => {
    if (!sessionId) return;

    let cancelled = false;
    let timeoutId: number | undefined;

    async function checkOrderStatus() {

      try {
        const response = await api.orderStatus(sessionId!);

        if (cancelled) return;

        setOrderInfo(response);
        setError(null);

        if (response.status === "PENDING" &&
          response.reviewNeededAt == null
        ) {
          timeoutId = window.setTimeout(
            checkOrderStatus,
            1500
          );
        }
      }
      catch (e) {
        if (cancelled) return;

        setError(getErrorMessage(e));
      }

    }

    void checkOrderStatus();

    return () => {
      cancelled = true;

      if (timeoutId !== undefined) {
        window.clearTimeout(timeoutId);
      }
    };
  }, [sessionId, retryCount]);

  function retryPaymentStatus() {
    setError(null);
    setOrderInfo(null);
    setRetryCount(count => count + 1);
  }

  const paymentConfirmed = orderInfo?.status === "PAID";
  const cancelled = orderInfo?.status === "CANCELLED";
  const expired = orderInfo?.status === "EXPIRED";
  const needsReview = orderInfo?.reviewNeededAt != null;

  if (!sessionId) {
    return (
      <main className="page-shell narrow">
        <div className="success-card">
          <h1>Invalid checkout session</h1>
          <p>No checkout session was provided.</p>

          <Link className="button large" to="/products">
            Continue shopping
          </Link>
        </div>
      </main>
    );
  }


  return (
    <main className="page-shell narrow">
      <div className="success-card">
        <p className="section-label">
          {error ? "ERROR" : orderInfo?.status ?? "CHECKING..."}</p>

        {
          error ? (
            <>
            <h1> Unable to check payment status </h1>
            <p className="error">{error}</p>
            <p>
              Your payment may have succeeded, do not retry payment yet.
            </p>
            <button
            type="button"
            className="button large"
            onClick={retryPaymentStatus} 
            >
              Check payment status
            </button>
            
            </>)
            : needsReview ? (
              <>
                <h1>Order is under review</h1>
                <p>Please wait, we will contact you when the review is complete </p>
              </>)
              : cancelled ? (
                <>
                  <h1>Order has been cancelled</h1>
                  <p>No payment was taken </p>
                </>)
                : paymentConfirmed ? (
                  <>
                    <h1>Payment confirmed</h1>
                    <p>Thank you for your order</p>
                  </>)

                  : expired ? (
                    <>
                      <h1>Order expired</h1>
                      <p>Please try again</p>
                    </>)
                    : (
                      <>
                        <h1>Confirming payment</h1>
                        <p>Please wait...</p>
                      </>)
        }


        {sessionId && <p className="tiny">Session: {sessionId}</p>}
        <Link className="button large" to="/products">Continue shopping</Link>
      </div>
    </main>
  );
}
