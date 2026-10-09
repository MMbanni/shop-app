import type { User } from "./user";

export type AuthContextValue = {
  user: User | null;
  isLoading: boolean;
  isLoggedIn: boolean;

  login: (
    email: string,
    password: string,
  ) => Promise<boolean>;

  register: (
    name: string,
    email: string,
    password: string,
  ) => Promise<boolean>;

  logout: () => void;
};