export interface LeaveRequest {
    id: string;
    employeeId: string;
    username:string;
    startDate: string;
    endDate: string;
    type: LeaveType;
    status: LeaveStatus;
    reason: string;
    createdAt: string;
    updatedAt: string;
  }
  
  export enum LeaveType {
    VACATION = 'VACATION',
    SICK = 'SICK',
    PERSONAL = 'PERSONAL',
    MATERNITY = 'MATERNITY',
    PATERNITY = 'PATERNITY',
    BEREAVEMENT = 'BEREAVEMENT'
  }
  
  export enum LeaveStatus {
    PENDING = 'PENDING',
    APPROVED = 'APPROVED',
    REJECTED = 'REJECTED',
    CANCELLED = 'CANCELLED'
  }
  
  export interface LeaveRequestDto {
    startDate: string;
    endDate: string;
    type: LeaveType;
    reason: string;
  }