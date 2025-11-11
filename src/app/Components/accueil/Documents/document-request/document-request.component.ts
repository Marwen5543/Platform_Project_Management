import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DocumentService } from 'src/app/Service/document.service';
import { KeycloakService } from 'src/app/Service/KeycloakService';
import { DocumentRequestDto, DocumentType } from 'src/app/Models/document-request';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-document-request',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule
  ],
  templateUrl: './document-request.component.html',
  styleUrls: ['./document-request.component.css']
})
export class DocumentRequestComponent implements OnInit {
  documentForm: FormGroup;
  documentTypes = Object.values(DocumentType);
  selectedType: DocumentType | null = null;
  canRequest = false;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private documentService: DocumentService,
    private keycloakService: KeycloakService,
    private snackBar: MatSnackBar
  ) {
    this.documentForm = this.fb.group({
      documentType: ['', Validators.required],
      monthYear: ['']
    });
  }

  async ngOnInit(): Promise<void> {
    console.log('Component initialized');
    const roles = await this.keycloakService.getRoles();
    this.canRequest = (roles.includes('EMPLOYEE') || roles.includes('MANAGER') || roles.includes('ADMIN')) &&
                     !roles.includes('HR') && !roles.includes('SUPER_ADMIN');
    if (!this.canRequest) {
      this.snackBar.open('Seuls les employés, managers ou administrateurs peuvent demander des documents', 'Fermer', { duration: 5000 });
      this.router.navigate(['/accueil']);
      return;
    }

    this.route.paramMap.subscribe(params => {
      const type = params.get('type');
      if (type) {
        const docType = this.mapRouteToDocumentType(type);
        this.selectedType = docType;
        this.documentForm.patchValue({ documentType: docType });
        this.updateMonthYearValidator();
      }
    });

    this.documentForm.get('documentType')?.valueChanges.subscribe(value => {
      this.selectedType = value;
      this.updateMonthYearValidator();
    });
  }

  private mapRouteToDocumentType(route: string): DocumentType {
    switch (route) {
      case 'fiche-paie': return DocumentType.PAYSLIP;
      case 'attestation-travail': return DocumentType.WORK_ATTESTATION;
      case 'certificat-travail': return DocumentType.CERTIFICATE;
      default: return DocumentType.PAYSLIP;
    }
  }

  private updateMonthYearValidator() {
    const monthYearControl = this.documentForm.get('monthYear');
    if (this.selectedType === DocumentType.PAYSLIP) {
      monthYearControl?.setValidators([Validators.required, Validators.pattern(/^\d{4}-\d{2}$/)]);
    } else {
      monthYearControl?.clearValidators();
    }
    monthYearControl?.updateValueAndValidity();
  }

  submitRequest() {
    if (this.documentForm.invalid) {
      this.snackBar.open('Veuillez remplir tous les champs requis', 'Fermer', { duration: 5000 });
      return;
    }

    const dto: DocumentRequestDto = {
      documentType: this.documentForm.value.documentType,
      monthYear: this.documentForm.value.monthYear || ''
    };

    this.documentService.requestDocument(dto).subscribe({
      next: () => {
        this.snackBar.open('Demande de document soumise', 'Fermer', { duration: 3000 });
        this.router.navigate(['/historique-documents']);
      },
      error: () => {
        this.snackBar.open('Erreur lors de la soumission de la demande', 'Fermer', { duration: 5000 });
      }
    });
  }

  
}