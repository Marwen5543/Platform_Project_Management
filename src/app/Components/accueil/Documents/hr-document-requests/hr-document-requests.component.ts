// src/app/hr-document-requests/hr-document-requests.component.ts
import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';
import { DocumentRequest, DocumentStatus } from 'src/app/Models/document-request';
import { DocumentService } from 'src/app/Service/document.service';

@Component({
  selector: 'app-hr-document-requests',
  standalone: true,
  imports: [CommonModule, MatTableModule, MatButtonModule],
  templateUrl: './hr-document-requests.component.html',
  styleUrl: './hr-document-requests.component.css'
})
export class HrDocumentRequestsComponent implements OnInit {
  displayedColumns: string[] = ['id', 'username', 'documentType', 'monthYear', 'status', 'createdAt', 'actions'];
  documentRequests: DocumentRequest[] = [];

  constructor(
    private documentService: DocumentService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadPendingRequests();
  }

  loadPendingRequests(): void {
    this.documentService.getPendingDocumentRequests().subscribe({
      next: (requests) => {
        this.documentRequests = requests;
      },
      error: (error: HttpErrorResponse) => {
        this.snackBar.open('Failed to load pending requests: ' + error.message, 'Close', { duration: 5000 });
      }
    });
  }

  approveDocument(id: string): void {
    this.documentService.approveDocument(id).subscribe({
      next: () => {
        this.snackBar.open('Document approved successfully', 'Close', { duration: 3000 });
        this.loadPendingRequests();
      },
      error: (error: HttpErrorResponse) => {
        this.snackBar.open('Failed to approve document: ' + error.message, 'Close', { duration: 5000 });
      }
    });
  }

  getStatusDisplayName(status: DocumentStatus): string {
    switch (status) {
      case DocumentStatus.REQUESTED: return 'Requested';
      case DocumentStatus.PENDING_APPROVAL: return 'Pending Approval';
      case DocumentStatus.GENERATED: return 'Generated';
      case DocumentStatus.FAILED: return 'Failed';
      case DocumentStatus.APPROVED: return 'Approved';
      default: return 'Unknown';
    }
  }
}