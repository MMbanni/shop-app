import { Navigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

export function AdminRoute({ children }: { children: React.ReactNode }) {
  const {user, isLoading, isLoggedIn } = useAuth();

  if (isLoading) {
    return <p className="page-message">Loading your account...</p>;
  }

  if (!isLoggedIn) {
    return <Navigate to="/login" replace />;
  }

if (user?.role != "ADMIN") {
    return <Navigate to="/" replace />;
  }


  return children;
}

