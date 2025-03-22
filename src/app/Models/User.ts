export interface User {
    userId?: number;  // '?' signifie que c'est optionnel (comme un champ auto-généré)
    username: string;
    email: string;
    password: string;
    role: UserRole;
    status: UserStatus;
  }
  
  export enum UserRole {
    ADMIN = 'ADMIN',
    EMPLOYEE = 'EMPLOYEE',
    MANAGER = 'MANAGER',
    HR = 'HR'
  }
  
  export enum UserStatus {
    ACTIVE = 'ACTIVE',
    INACTIVE = 'INACTIVE'
  }
  