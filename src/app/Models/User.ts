export interface User {
  userId: string;
  username: string;
  email: string;
  role: string;
  status: string;
  firstName?: string;
  lastName?: string;
  phone?: string;
  address?: string;
  hireDate?: string | Date;
  departmentId?: number;
  managerId?: number;
  position?: string;
  imageUrl?: string;
  forcePasswordReset?: boolean;
}

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