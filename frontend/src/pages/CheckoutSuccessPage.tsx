import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { api } from "../lib/api";
import { ApiErrorResponse, OrderStatus } from "../types";
import { ApiError, getApiError } from "../lib/ApiError";



export function CheckoutSuccessPage() {
  const [searchParams] = useSearchParams();
  const sessionId = searchParams.get("session_id");
  const [orderStatus, setOrderStatus] = useState<OrderStatus | null>(null);
  const [apiError, setApiError] = useState<ApiErrorResponse | null>(null);


  useEffect(() => {
    if (!sessionId) {
      return;
    }

    let cancelled = false;
    let timeoutId: number | undefined;

    async function checkOrderStatus() {

      try {
        const response = await api.orderStatus(sessionId!);

        setOrderStatus(response.status);

        if (response.status === "PENDING") {
          timeoutId = window.setTimeout(
            checkOrderStatus,
            1500
          );
        }
      }
      catch (e) {
        setApiError(e as ApiErrorResponse)

        return
      }

      if (cancelled) {
        return;
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
  const cancelled = orderStatus === "CANCELLED";
  const expired = orderStatus === "EXPIRED";
  const notFound = getApiError(apiError)?.status == 404;



  return (
    <main className="page-shell narrow">
      <div className="success-card">
        <p className="section-label">{apiError ? "" : orderStatus ?? "CHECKING..."}</p>

        {
          apiError ?
            
              (<h1> {`${getApiError(apiError)?.detail}`} </h1>) 
              : cancelled ? (
                <><h1>Order has been cancelled</h1> <p> No payment was taken </p>
                </>)
                : paymentConfirmed ? (
                  <><h1>Payment confirmed</h1> <p> Thank you for your order</p>
                  </>)
                  : (
                    <><h1>Confirming payment</h1> <p> Please wait... </p>
                    </>)
        }


        {sessionId && <p className="tiny">Session: {sessionId}</p>}
        <Link className="button large" to="/products">Continue shopping</Link>
      </div>
    </main>
  );
}
