import { Link } from "react-router-dom";
import "./footer.css"
export function Footer() {
  return (
    <footer className="site-footer">
      <div className="footer-content">
        

        <nav className="footer-links" aria-label="Footer navigation">
          <div>
            <h3>Shop</h3>
            <Link to="/products">Products</Link>
            <Link to="/cart">Cart</Link>
          </div>

          <div>
            <h3>Company</h3>
            <Link to="/about">About</Link>
            <Link to="/contact">Contact us</Link>
          </div>
        </nav>
      </div>

      <div className="footer-bottom">
        <span>© {new Date().getFullYear()} ShopApp</span>
      </div>
    </footer>
  );
}