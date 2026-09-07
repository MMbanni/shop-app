import { ReactNode } from "react";

export type User = {
  id: number;
  email: string;
  name: string;
  role?: Role;
  status: string,
  suspendedUntil: string | null
};

export type Role = "ADMIN" | "CUSTOMER";

export type UserStatus = "ACTIVE" | "INACTIVE" | "SUSPENDED" | "BANNED";
export type UpdateUserStatusRequest = {
  id: number,
  status:UserStatus,
  duration:number
}

export type SuspendUserForm = {
  id: number,
  duration:number
}
