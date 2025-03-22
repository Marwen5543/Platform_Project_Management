export interface UserDTO {
    userId: number;
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
  }
  
  export interface LoginResponse {
    accessToken: string;
    tokenType: string;
    username: string;
    role: string;
  }

    
  export interface LoginRequest {
    username: string;
    password: string;
  }