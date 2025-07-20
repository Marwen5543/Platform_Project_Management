export enum UserRole {
  SUPER_ADMIN = 'SUPER_ADMIN',
  ADMIN = 'ADMIN',
  EMPLOYEE = 'EMPLOYEE',
  MANAGER = 'MANAGER',
  HR = 'HR'
}

export enum UserStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE'
}

export interface UserDTO {
  userId: string;
  username: string;
  email: string;
  role: UserRole;
  roles?: string[];
  status: UserStatus;
  firstName?: string;
  lastName?: string;
  phone?: string;
  address?: string;
  hireDate?: string | Date| null | undefined ;
  departmentId?: number;
  managerId?: number;
  position?: string;
  imageUrl?: string;
  isOnline?: boolean;          
  emailVerified?: boolean;     
  twoFactorEnabled?: boolean;  
  joinDate?: string | Date | null | undefined;    
  projectTitles?: string[];
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  forcePasswordReset: boolean;
  username: string;
  role: UserRole;  // Use enum for type safety
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

export interface CreateUserRequest {
  username: string;
  email: string;
  password: string;
  role: string;
  firstName?: string;
  lastName?: string;
  phone?: string;
  address?: string;
  hireDate?: string | Date;
  departmentId?: number;
  managerId?: number;
  position?: string;
}