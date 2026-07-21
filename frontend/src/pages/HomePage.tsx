import { Link } from "react-router-dom";

export function HomePage() {
  return (
    <main className="hero page-shell">
      <section className="hero-content">
        <p className="eyebrow">Spring Boot + React portfolio project</p>
        <h1>Shop app</h1>
        <p className="hero-text">
          Products, login, protected cart pages, JWT auth, and Stripe Checkout.
        </p>
        <div className="hero-actions">
          <Link className="button large" to="/products">Shop products</Link>
          <Link className="button ghost large" to="/cart">View cart</Link>
        </div>
      </section>

      <section className="hero-panel">
        <div className="metric-card">
          <span>Frontend</span>
          <strong>React + TypeScript</strong>
        </div>
        <div className="metric-card">
          <span>Backend</span>
          <strong>Spring Boot API</strong>
        </div>
        <div className="metric-card">
          <span>Payment</span>
          <strong>Stripe Checkout</strong>
        </div>
      </section>
    </main>
  );
}
