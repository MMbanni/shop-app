import { Link, NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

/*

function Navbar(props: { title: string }) {
  return (
    <header>
      <h1>{props.title}</h1>
    </header>
  );
}

function Navbar({props, children}: {props: prop, children:React.ReactNode}) {
  return(
    <p> Something </p>
  )
}

*/

export function Navbar() {
  const { user, isLoggedIn, logout } = useAuth();
  const navigate = useNavigate();
  

  function handleLogout() {
    logout();
    navigate("/");
  }

  return (
    <header className="navbar">
      <Link to="/" className="brand">
        <span className="brand-mark">H</span>
        Shop
      </Link>

      <nav className="nav-links">
        {user?.role === "ADMIN" && <NavLink to="/admin">Admin</NavLink>}
        <NavLink to="/products">Products</NavLink>
        <NavLink to="/cart">Cart</NavLink>        
        
      </nav>

      <div className="nav-actions">
        {isLoggedIn ? (
          <>
            <span className="hello">Hi, {user?.name}</span>
            <button className="button ghost" onClick={handleLogout}>Logout</button>
          </>
        ) : (
          <>
            <Link className="button ghost" to="/login">Login</Link>
            <Link className="button" to="/register">Register</Link>
          </>
        )}
      </div>
    </header>
  );
}
