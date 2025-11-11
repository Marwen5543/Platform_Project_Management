import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule, DatePipe } from '@angular/common';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router'; // Import Router for redirection
import { LeaveRequestDto, LeaveType } from 'src/app/Models/LeaveRequest';
import { LeaveService } from 'src/app/Service/LeaveService';
import { KeycloakService } from 'src/app/Service/KeycloakService'; // Import KeycloakService
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

// Interface for leave balance display
interface LeaveBalance {
  value: number;
  label: string;
  icon: string;
}

@Component({
  selector: 'app-leave-request',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatSelectModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './leave-request.component.html',
  styleUrls: ['./leave-request.component.css'],
  providers: [DatePipe]
})
export class LeaveRequestComponent implements OnInit {
  leaveForm: FormGroup;
  rangeForm: FormGroup;
  leaveTypes = Object.values(LeaveType);
  isLoading = false;
  canRequestLeave = false; // New property to track if user can request leave
  isLoaded = false;


  leaveTypeLabels: { [key in LeaveType]: string } = {
    [LeaveType.VACATION]: 'Congé payé',
    [LeaveType.SICK]: 'Congé maladie',
    [LeaveType.PERSONAL]: 'Congé personnel',
    [LeaveType.MATERNITY]: 'Congé maternité',
    [LeaveType.PATERNITY]: 'Congé paternité',
    [LeaveType.BEREAVEMENT]: 'Congé de deuil'
  };

  // Static leave balances for display (replace with actual data from backend if needed)
  leaveBalances: LeaveBalance[] = [
    { value: 25, label: 'Payés', icon: 'beach_access' },
    { value: 10, label: 'RTT', icon: 'schedule' },
    { value: 5, label: 'Maladie', icon: 'local_hospital' }
  ];

  constructor(
    private fb: FormBuilder,
    private leaveService: LeaveService,
    private snackBar: MatSnackBar,
    private datePipe: DatePipe,
    private keycloakService: KeycloakService, // Add KeycloakService
    private router: Router // Add Router
  ) {
    this.rangeForm = this.fb.group({
      start: ['', Validators.required],
      end: ['', Validators.required]
    });

    this.leaveForm = this.fb.group({
      range: this.rangeForm,
      type: ['', Validators.required],
      reason: ['', [Validators.minLength(10)]] // Made optional but with min length when provided
    });
  }

  async ngOnInit(): Promise<void> {
    // Check user roles for leave request permission
    const roles = await this.keycloakService.getRoles();
    this.canRequestLeave = (roles.includes('EMPLOYEE') || roles.includes('MANAGER') || roles.includes('HR')) &&
                          !roles.includes('ADMIN') && !roles.includes('SUPER_ADMIN');
    if (!this.canRequestLeave) {
      this.snackBar.open('Seuls les employés, managers peuvent demander des congés', 'Fermer', { duration: 5000 });
      this.router.navigate(['/accueil']);
      return;
    }

    // Set minimum date to today
    const today = new Date();
    this.rangeForm.get('start')?.valueChanges.subscribe(value => {
      if (value) {
        this.rangeForm.get('end')?.setValidators([Validators.required]);
        this.rangeForm.get('end')?.updateValueAndValidity();
      }
    });

    setTimeout(() => {
  this.isLoaded = true;
}, 100);
  }

  getLeaveTypeLabel(type: string): string {
    return this.leaveTypeLabels[type as LeaveType] || type;
  }

  // Method to get icon for leave type
  getLeaveTypeIcon(type: string): string {
    const iconMap: { [key in LeaveType]: string } = {
      [LeaveType.VACATION]: 'beach_access',
      [LeaveType.SICK]: 'local_hospital',
      [LeaveType.PERSONAL]: 'person',
      [LeaveType.MATERNITY]: 'pregnant_woman',
      [LeaveType.PATERNITY]: 'family_restroom',
      [LeaveType.BEREAVEMENT]: 'sentiment_very_dissatisfied'
    };
    return iconMap[type as LeaveType] || 'event';
  }

  // Method to show summary section
  showSummary(): boolean {
    return !!(this.rangeForm.value.start && this.rangeForm.value.end);
  }

  // Calculate total business days (excluding weekends)
  calculateTotalDays(): number {
    if (this.rangeForm.valid && this.rangeForm.value.start && this.rangeForm.value.end) {
      const start = new Date(this.rangeForm.value.start);
      const end = new Date(this.rangeForm.value.end);
      
      let count = 0;
      const currentDate = new Date(start);
      
      while (currentDate <= end) {
        const dayOfWeek = currentDate.getDay();
        // Count business days (Monday = 1, Friday = 5)
        if (dayOfWeek >= 1 && dayOfWeek <= 5) {
          count++;
        }
        currentDate.setDate(currentDate.getDate() + 1);
      }
      
      return count;
    }
    return 0;
  }

  onSubmit() {
    if (this.leaveForm.valid && this.rangeForm.valid) {
      this.isLoading = true;
      const formValue = this.leaveForm.value;

      const dto: LeaveRequestDto = {
        startDate: this.datePipe.transform(formValue.range.start, 'yyyy-MM-dd')!,
        endDate: this.datePipe.transform(formValue.range.end, 'yyyy-MM-dd')!,
        type: formValue.type,
        reason: formValue.reason || '' // Handle optional reason
      };

      this.leaveService.requestLeave(dto).subscribe({
        next: () => {
          this.leaveForm.reset();
          this.rangeForm.reset();
          this.isLoading = false;
          this.snackBar.open('Demande de congé soumise avec succès !', 'Fermer', {
            duration: 3000,
            panelClass: ['success-snackbar']
          });
        },
        error: (err) => {
          this.isLoading = false;
          this.snackBar.open(`Erreur : ${err.message}`, 'Fermer', {
            duration: 5000,
            panelClass: ['error-snackbar']
          });
        }
      });
    } else {
      // Mark all fields as touched to show validation errors
      this.markFormGroupTouched(this.leaveForm);
      this.markFormGroupTouched(this.rangeForm);
      
      this.snackBar.open('Veuillez remplir tous les champs obligatoires', 'Fermer', {
        duration: 3000,
        panelClass: ['error-snackbar']
      });
    }
  }

  // Helper method to mark all form controls as touched
  private markFormGroupTouched(formGroup: FormGroup) {
    Object.keys(formGroup.controls).forEach(field => {
      const control = formGroup.get(field);
      if (control instanceof FormGroup) {
        this.markFormGroupTouched(control);
      } else {
        control?.markAsTouched({ onlySelf: true });
      }
    });
  }
}