export type User = {
  id: number;
  email: string;
  name: string;
  role?: Role;
  status: string
};

export type Role = "ADMIN" | "CUSTOMER";

export type UserStatus = "ACTIVE" | "INACTIVE" | "SUSPENDED" | "BANNED";
export type UpdateUserStatusRequest = {
  id: number,
  status:UserStatus,
  duration:number
}

