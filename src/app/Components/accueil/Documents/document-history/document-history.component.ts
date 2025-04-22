import { Component, OnInit } from '@angular/core';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { CommonModule, DatePipe } from '@angular/common';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DocumentService } from 'src/app/Service/document.service';

@Component({
  selector: 'app-document-history',
  standalone: true,
  imports: [CommonModule, MatTableModule, MatButtonModule, DatePipe],
  templateUrl: './document-history.component.html',
  styleUrls: ['./document-history.component.css']
})
export class DocumentHistoryComponent implements OnInit {
  displayedColumns: string[] = ['type', 'period', 'status', 'date', 'actions'];
  documents: any[] = [];

  constructor(
    private documentService: DocumentService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadDocuments();
  }

  loadDocuments(): void {
    this.documentService.getDocumentHistory().subscribe({
      next: (docs) => this.documents = docs,
      error: (err) => this.snackBar.open('Failed to load documents', 'Close', { duration: 5000 })
    });
  }

  downloadDocument(id: string, type: string, period: string): void {
    this.documentService.downloadDocument(id, type, period).subscribe({
      error: (err) => this.snackBar.open('Download failed', 'Close', { duration: 5000 })
    });
  }

  getStatusDisplay(status: string): string {
    const statusMap: Record<string, string> = {
      'PENDING_APPROVAL': 'Pending Approval',
      'APPROVED': 'Ready for Download',
      'FAILED': 'Generation Failed',
      'GENERATED': 'Ready for Download'
    };
    return statusMap[status] || status;
  }

  getTypeDisplay(type: string): string {
    const typeMap: Record<string, string> = {
      'PAYSLIP': 'Payslip',
      'WORK_ATTESTATION': 'Work Attestation',
      'CERTIFICATE': 'Certificate'
    };
    return typeMap[type] || type;
  }
}