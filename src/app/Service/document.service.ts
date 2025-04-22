import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, from } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';
import { MatSnackBar } from '@angular/material/snack-bar';
import { KeycloakService } from './KeycloakService';
import { DocumentRequest, DocumentRequestDto } from '../Models/document-request';
import { saveAs } from 'file-saver';

@Injectable({
  providedIn: 'root',
})
export class DocumentService {
  private apiUrl = 'http://localhost:8090/api/documents';

  constructor(
    private http: HttpClient,
    private keycloakService: KeycloakService,
    private snackBar: MatSnackBar
  ) {}

  private async getAuthHeaders(): Promise<HttpHeaders> {
    const token = await this.keycloakService.getToken();
    if (!token) {
      throw new Error('No authentication token available');
    }
    return new HttpHeaders({
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  private async isHR(): Promise<boolean> {
    const roles = await this.keycloakService.getRoles();
    return roles.includes('HR');
  }

  requestDocument(dto: DocumentRequestDto): Observable<DocumentRequest> {
    return from(this.getAuthHeaders()).pipe(
      switchMap(headers =>
        this.http.post<DocumentRequest>(`${this.apiUrl}/request`, dto, { headers })
      ),
      tap(() => this.snackBar.open('Document requested successfully', 'Close', { duration: 3000 })),
      catchError(this.handleError.bind(this))
    );
  }

  getDocumentHistory(): Observable<DocumentRequest[]> {
    return from(this.getAuthHeaders()).pipe(
      switchMap(headers =>
        this.http.get<DocumentRequest[]>(`${this.apiUrl}/history`, { headers })
      ),
      catchError(this.handleError.bind(this))
    );
  }

   // Enhanced downloadDocument method with filename handling
   downloadDocument(id: string, documentType?: string, monthYear?: string): Observable<Blob> {
    return from(this.getAuthHeaders()).pipe(
      switchMap(headers =>
        this.http.get(`${this.apiUrl}/download/${id}`, {
          headers,
          responseType: 'blob',
          observe: 'response'
        })
      ),
      tap(response => {
        const contentDisposition = response.headers.get('Content-Disposition');
        let filename = `document_${id}.pdf`;
        
        if (contentDisposition) {
          const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
          if (filenameMatch && filenameMatch[1]) {
            filename = filenameMatch[1].replace(/['"]/g, '');
          }
        } else if (documentType && monthYear) {
          filename = `${documentType.toLowerCase()}_${monthYear}.pdf`;
        } else if (documentType) {
          filename = `${documentType.toLowerCase()}.pdf`;
        }
        
        this.saveFile(response.body as Blob, filename);
        this.snackBar.open('Document downloaded successfully', 'Close', { duration: 3000 });
      }),
      switchMap(response => [response.body as Blob]),
      catchError(this.handleError.bind(this))
    );
  }
  private saveFile(blob: Blob, filename: string): void {
    const a = document.createElement('a');
    const url = URL.createObjectURL(blob);
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    setTimeout(() => {
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    }, 0);
  }

  getPendingDocumentRequests(): Observable<DocumentRequest[]> {
    return from(this.getAuthHeaders()).pipe(
      switchMap(headers =>
        this.http.get<DocumentRequest[]>(`${this.apiUrl}/pending`, { headers })
      ),
      catchError(this.handleError.bind(this))
    );
  }

  approveDocument(id: string): Observable<DocumentRequest> {
    return from(Promise.all([this.getAuthHeaders(), this.isHR()])).pipe(
      switchMap(([headers, isHR]) => {
        if (!isHR) {
          this.snackBar.open('Only HR can approve documents', 'Close', { duration: 5000 });
          return throwError(() => new Error('Unauthorized: HR role required'));
        }
        return this.http.post<DocumentRequest>(`${this.apiUrl}/approve/${id}`, {}, { headers });
      }),
      tap(() => this.snackBar.open('Document approved successfully', 'Close', { duration: 3000 })),
      catchError(this.handleError.bind(this))
    );
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'An unknown error occurred';
    if (error.error instanceof ErrorEvent) {
      errorMessage = `Client-side error: ${error.error.message}`;
    } else {
      errorMessage = error.error?.message || error.message;
      switch (error.status) {
        case 400:
          errorMessage = 'Invalid request data';
          break;
        case 401:
          this.keycloakService.login();
          errorMessage = 'Session expired. Redirecting to login...';
          break;
        case 403:
          errorMessage = 'You do not have permission to perform this action';
          break;
        case 404:
          errorMessage = `Document endpoint not found at ${error.url}. Check backend service.`;
          break;
        case 422:
          errorMessage = 'Invalid document state';
          break;
      }
    }
    this.snackBar.open(errorMessage, 'Close', { duration: 5000 });
    return throwError(() => new Error(errorMessage));
  }
}