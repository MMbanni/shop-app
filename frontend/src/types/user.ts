export type User = {
  id: number;
  email: string;
  name: string;
  role?: Role;
};

export type Role = "ADMIN" | "CUSTOMER";

