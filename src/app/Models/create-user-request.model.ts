export interface CreateUserRequest {
  username: string;
  email: string;
  password: string;
  role: string;
  firstName?: string; // New optional fields
  lastName?: string;
  phone?: string;
  address?: string;
  hireDate?: string | Date;
  departmentId?: number;
  managerId?: number;
  position?: string;
}