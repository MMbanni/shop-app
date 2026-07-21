import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useMutation } from "@tanstack/react-query";
import { ApiErrorMessage } from "../components/messages/ApiErrorMessage";

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const loginMutation = useMutation({
    mutationFn: ({ email, password }: { email:string, password:string }) =>
      login(email, password),

    onSuccess: () => {
      navigate("/products");
    }
  });

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    loginMutation.mutate({
      email,
      password
    });
  }

  return (
    <main className="auth-page">
      <form className="auth-card" onSubmit={handleSubmit}>
        <p className="eyebrow">Welcome back</p>
        <h1>Login</h1>

        <label>
          Email
          <input 
          name="email"
          type="email"
          value={email}
          autoComplete="email"
          disabled={loginMutation.isPending}
          onChange={(event) => setEmail(event.target.value)} required 
          />
        </label>

        <label>
          Password
          <input
          name="password" 
          type="password" 
          value={password}
          disabled={loginMutation.isPending}
          onChange={(event) => setPassword(event.target.value)}
          required
          />
        </label>

        {loginMutation.isError && (
          <ApiErrorMessage
            error={loginMutation.error}
            fallback="Login failed."
          />
        )}

        <button
          className="button large full"
          disabled={loginMutation.isPending}
          >
          {loginMutation.isPending ? "Logging in..." : "Login"}
        </button>

        <p className="muted center">
          No account? <Link to="/register">Create one</Link>
        </p>
      </form>
    </main>
  );
}
