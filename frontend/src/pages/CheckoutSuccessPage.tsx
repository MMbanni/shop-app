import { Link, useSearchParams } from "react-router-dom";

export function CheckoutSuccessPage() {
  const [searchParams] = useSearchParams();
  const sessionId = searchParams.get("session_id");

  return (
    <main className="page-shell narrow">
      <div className="success-card">
        <p className="eyebrow">Payment complete</p>
        <h1>Thank you for your order.</h1>
        
        {sessionId && <p className="tiny">Session: {sessionId}</p>}
        <Link className="button large" to="/products">Continue shopping</Link>
      </div>
    </main>
  );
}
