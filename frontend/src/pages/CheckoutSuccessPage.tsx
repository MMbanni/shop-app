import { useEffect } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { api } from "../lib/api";

export function CheckoutSuccessPage() {
  const [searchParams] = useSearchParams();
  const sessionId = searchParams.get("session_id");

  useEffect(()=>{
    if (sessionId == null) return;

    api.orderStatus(sessionId)
  }, [sessionId, ]);

  return (
    <main className="page-shell narrow">
      <div className="success-card">
        <p className="section-label">Payment complete</p>
        <h1>Thank you for your order.</h1>
        
        {sessionId && <p className="tiny">Session: {sessionId}</p>}
        <Link className="button large" to="/products">Continue shopping</Link>
      </div>
    </main>
  );
}
