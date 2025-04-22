import { Component, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DocumentRequest } from 'src/app/Models/document-request';
import { DocumentService } from 'src/app/Service/document.service';
import { saveAs } from 'file-saver';
import { CommonModule } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { DatePipe, TitleCasePipe } from '@angular/common';

@Component({
  selector: 'app-document',
  standalone: true,
  imports: [
    CommonModule,
    MatProgressSpinnerModule,
    MatTableModule,
    MatButtonModule,
    DatePipe,
    TitleCasePipe
  ],
  templateUrl: './document.component.html',
  styleUrls: ['./document.component.css']
})
export class DocumentComponent implements OnInit {
  documents: DocumentRequest[] = [];
  isLoading = true;
  displayedColumns: string[] = ['type', 'period', 'status', 'date', 'actions'];

  constructor(
    private documentService: DocumentService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadDocumentHistory();
  }

  loadDocumentHistory(): void {
    this.documentService.getDocumentHistory().subscribe({
      next: (documents) => {
        this.documents = documents;
        this.isLoading = false;
      },
      error: (err) => {
        this.snackBar.open('Failed to load document history', 'Close', { duration: 5000 });
        this.isLoading = false;
      }
    });
  }

  downloadDocument(document: DocumentRequest): void {
    if (document.status !== 'APPROVED' || !document.id) {
      this.snackBar.open('Document not available for download', 'Close', { duration: 3000 });
      return;
    }

    this.documentService.downloadDocument(document.id).subscribe({
      next: (blob) => {
        const filename = this.generateFilename(document);
        saveAs(blob, filename);
      },
      error: (err) => {
        this.snackBar.open('Failed to download document', 'Close', { duration: 5000 });
      }
    });
  }

  private generateFilename(document: DocumentRequest): string {
    let filename = `${document.documentType.toLowerCase()}`;
    if (document.monthYear) {
      filename += `_${document.monthYear}`;
    }
    filename += `_${document.id}.pdf`;
    return filename;
  }
}