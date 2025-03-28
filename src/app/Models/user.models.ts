export interface UserDTO {
  userId: string;
  username: string;
  email: string;
  role: string;
  status: string;
  // New optional properties
  isOnline?: boolean;
  fullName?: string;
  emailVerified?: boolean;
  joinDate?: Date | string;
  twoFactorEnabled?: boolean;
  firstName?: string;
  lastName?: string;
  phone?: string;
  address?: string;
  hireDate?: string | Date;
  departmentId?: number;
  managerId?: number;
  position?: string;
  imageUrl?: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  forcePasswordReset: boolean;
  username: string;
  role: string;
  firstName?: string;
  lastName?: string;
  phone?: string;
  address?: string;
  hireDate?: string;
  departmentId?: number;
  managerId?: number;
  position?: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}