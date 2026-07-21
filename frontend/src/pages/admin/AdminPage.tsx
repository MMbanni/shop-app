import { NavLink} from "react-router-dom";
import { useAuth } from "../../context/AuthContext";


export function AdminPage() {

  return (
    <main className="page-shell narrow">
      <div className="page-heading">
        <p className="eyebrow">Admin</p>
        <h1>Admin portal</h1>
        <p className="muted">List of admin actions</p>
      </div>

      <section className="admin-panel">
        <nav>
            <NavLink className="button" to="/admin/products">Products</NavLink>
            <NavLink className="button" to="/admin/users">Users</NavLink>
        </nav>

      </section>


    </main>
  );
}
