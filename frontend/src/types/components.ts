export type AdminProductTab = "ACTIVE" | "INACTIVE" | "ARCHIVED" | "ALL";
export type AdminUserTab = "ACTIVE" | "INACTIVE" | "SUSPENDED" | "BANNED" | "ALL";
export type ButtonMessage = {
  text: string;
  anchor: HTMLButtonElement;
};