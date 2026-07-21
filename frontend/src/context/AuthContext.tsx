import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { api } from "../lib/api";
import { clearToken, getToken, saveToken } from "../lib/token";
import type { User } from "../types";
import { useQueryClient } from "@tanstack/react-query";
import { AuthContextValue } from "../types/auth";

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const queryClient = useQueryClient();

  useEffect(() => {
    async function loadCurrentUser() {

      if (!getToken()) {
        setIsLoading(false);
        return;
      }

      try {
        const currentUser = await api.me();
        setUser(currentUser);
      } catch {
        clearToken();
        setUser(null);
        queryClient.clear()
      } finally {
        setIsLoading(false);
      }
    }

    void loadCurrentUser();
  }, [queryClient]);

  async function login(email: string, password: string) {
    const response = await api.login(email, password);

    saveToken(response.token);

    try {
      const currentUser = await api.me();
      setUser(currentUser);
    } catch (error) {
      clearToken();
      setUser(null);
      queryClient.clear();

      throw error;
    }
    
  }

  async function register(name: string, email: string, password: string) {
    await api.register(name, email, password);
    await login(email, password);
  }


  function logout() {
    clearToken();
    setUser(null);
    queryClient.clear();
  }


  const value: AuthContextValue = {
  user,
  isLoading,
  isLoggedIn: Boolean(user),
  login,
  register,
  logout
};

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }

  return context;
}
