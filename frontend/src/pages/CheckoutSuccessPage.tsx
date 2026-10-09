import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { api } from "../lib/api";
import { ApiErrorResponse, Order, OrderStatus } from "../types";
import { ApiError, getApiError } from "../lib/ApiError";



export function CheckoutSuccessPage() {
  const [searchParams] = useSearchParams();
  const sessionId = searchParams.get("session_id");
  const [orderInfo, setOrderInfo] = useState<Order | null>(null);
  const [apiError, setApiError] = useState<ApiErrorResponse | null>(null);


  useEffect(() => {
    if (!sessionId) {
      return;
    }
    let timeoutId: number | undefined;

    async function checkOrderStatus() {

      try {
        const response = await api.orderStatus(sessionId!);

        setOrderInfo(response);

        if (response.status === "PENDING") {
          timeoutId = window.setTimeout(
            checkOrderStatus,
            1500
          );
        }
      }
      catch (e) {
        const error = getApiError(e)
        if (error) {
          setApiError(error);
        }
        return;
      }

    }

    void checkOrderStatus();

    return () => {

      if (timeoutId !== undefined) {
        window.clearTimeout(timeoutId);
      }
    };
  }, [sessionId]);

  const paymentConfirmed = orderInfo?.status === "PAID";
  const cancelled = orderInfo?.status === "CANCELLED";
  const expired = orderInfo?.status === "EXPIRED";
  const needsReview = orderInfo?.reviewNeededAt !=null; 

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
        <p className="section-label">{apiError ? "" : orderInfo?.status ?? "CHECKING..."}</p>

        {
          apiError ?
            (<h1> {`${apiError.detail}`} </h1>)
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
