import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { useQueryClient } from "@tanstack/react-query";
import { api } from "../lib/api";
import { clearToken, getToken, saveToken } from "../lib/token";
import type { User } from "../types";
import type { AuthContextValue } from "../types/auth";

const AuthContext = createContext<AuthContextValue | null>(null);
const FORCED_LOGOUT_EVENT = "auth:forced-logout";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const queryClient = useQueryClient();
  const authGeneration = useRef(0);

  const logout = useCallback(() => {
    authGeneration.current++;

    clearToken();
    setUser(null);
    setIsLoading(false);
    queryClient.clear();
  }, [queryClient]);

  useEffect(() => {
    window.addEventListener(FORCED_LOGOUT_EVENT, logout);

    return () => {
      window.removeEventListener(FORCED_LOGOUT_EVENT, logout);
    };
  }, [logout]);

  useEffect(() => {
    const generation = authGeneration.current;

    async function loadCurrentUser() {
      if (!getToken()) {
        setIsLoading(false);
        return;
      }

      try {
        const currentUser = await api.me();

        if (generation !== authGeneration.current) return;

        setUser(currentUser);
      } catch {
        if (generation !== authGeneration.current) return;

        clearToken();
        setUser(null);
        queryClient.clear();
      } finally {
        if (generation === authGeneration.current) {
          setIsLoading(false);
        }
      }
    }

    void loadCurrentUser();

    return () => {
      // Invalidate pending startup, login and registration requests.
      authGeneration.current++;
    };
  }, [queryClient]);

  const login = useCallback(
    async (email: string, password: string): Promise<boolean> => {
      const generation = ++authGeneration.current;

      clearToken();
      setUser(null);
      queryClient.clear();

      try {
        const response = await api.login(email, password);

        if (generation !== authGeneration.current) return false;

        saveToken(response.token);

        const currentUser = await api.me();

        if (generation !== authGeneration.current) return false;

        setUser(currentUser);
        return true;
      } catch (error) {
        if (generation !== authGeneration.current) return false;

        clearToken();
        setUser(null);
        queryClient.clear();

        throw error;
      } finally {
        if (generation === authGeneration.current) {
          setIsLoading(false);
        }
      }
    },
    [queryClient],
  );

  const register = useCallback(
    async (
      name: string,
      email: string,
      password: string,
    ): Promise<boolean> => {
      const generation = ++authGeneration.current;

      clearToken();
      setUser(null);
      queryClient.clear();

      try {
        await api.register(name, email, password);
      } catch (error) {
        if (generation !== authGeneration.current) return false;

        setIsLoading(false);
        throw error;
      }

      if (generation !== authGeneration.current) return false;

      return login(email, password);
    },
    [login, queryClient],
  );

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isLoading,
      isLoggedIn: Boolean(user),
      login,
      register,
      logout,
    }),
    [user, isLoading, login, register, logout],
  );

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }

  return context;
}