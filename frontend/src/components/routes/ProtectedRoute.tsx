import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

export function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const {isLoading, isLoggedIn } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return <p className="page-message">Loading your account...</p>;
  }

  if (!isLoggedIn) {
    return <Navigate 
      to="/login" 
      state={{ returnTo: location.pathname + location.search }}
      replace />;
  }

  return children;
}
