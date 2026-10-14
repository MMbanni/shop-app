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
import { clearToken, getToken, saveToken, TOKEN_KEY } from "../lib/token";
import type { User } from "../types";
import type { AuthContextValue } from "../types/auth";
import { getErrorMessage } from "../lib/ApiError";

const AuthContext = createContext<AuthContextValue | null>(null);
const FORCED_LOGOUT_EVENT = "auth:forced-logout";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [startupError, setStartupError] = useState<string | null>(null);

  const invalidateAuthRequests = useCallback(() => {
    authGeneration.current++;
  }, []);

  useEffect(() => {
    function handleStorage(event: StorageEvent) {
      if (
        event.storageArea === localStorage &&
        (event.key === TOKEN_KEY || event.key === null)
      ) {
        window.location.reload();
      }
    }

    window.addEventListener("storage", handleStorage);

    return () => {
      window.removeEventListener("storage", handleStorage);
    };
  }, []);

  const queryClient = useQueryClient();
  const authGeneration = useRef(0);

  const logout = useCallback(() => {
    invalidateAuthRequests();
    setStartupError(null);

    clearToken();
    setUser(null);
    setIsLoading(false);
    queryClient.clear();
  }, [queryClient, invalidateAuthRequests]);

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
      } catch (error) {
        if (generation !== authGeneration.current) return;

        setStartupError(getErrorMessage(error))

      } finally {
        if (generation === authGeneration.current) {
          setIsLoading(false);
        }
      }
    }

    void loadCurrentUser();

    return invalidateAuthRequests;
  }, [invalidateAuthRequests]);

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

      try {
        return await login(email, password);
      } catch {
        throw new Error(
          "Your account was created, but automatic login failed. " +
          "Please use the Login page."
        );
      }
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

  if (startupError) {
    return (
      <main className="page-shell narrow">
        <h1>Unable to restore your session</h1>

        <p className="error" role="alert">
          {startupError}
        </p>

        <button
          className="button"
          onClick={() => window.location.reload()}
        >
          Retry
        </button>

        <button className="button ghost" onClick={logout}>
          Sign out
        </button>
      </main>
    );
  }

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