export interface DocumentRequest {
    id: string;
    employeeId: string;
    documentType: DocumentType;
    monthYear: string;
    status: DocumentStatus;
    filePath?: string;
    createdAt: string;
    username?: string;
}

export interface DocumentRequestDto {
    documentType: DocumentType;
    monthYear: string;
}

  // document-types.enum.ts
  export enum DocumentType {
    PAYSLIP = 'PAYSLIP',
    WORK_ATTESTATION = 'WORK_ATTESTATION',
    CERTIFICATE = 'CERTIFICATE'
}
  
export enum DocumentStatus {
    REQUESTED = 'REQUESTED',
    PENDING_APPROVAL = 'PENDING_APPROVAL',
    GENERATED = 'GENERATED',
    FAILED = 'FAILED',
    APPROVED = 'APPROVED'
}