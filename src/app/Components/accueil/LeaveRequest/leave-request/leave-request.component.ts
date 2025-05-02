import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule, DatePipe } from '@angular/common';
import { MatSnackBar } from '@angular/material/snack-bar';
import { LeaveRequestDto, LeaveType } from 'src/app/Models/LeaveRequest';
import { LeaveService } from 'src/app/Service/LeaveService';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

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
export class LeaveRequestComponent {
  leaveForm: FormGroup;
  rangeForm: FormGroup;
  leaveTypes = Object.values(LeaveType);
  isLoading = false;

  leaveTypeLabels: { [key in LeaveType]: string } = {
    [LeaveType.VACATION]: 'Congé payé',
    [LeaveType.SICK]: 'Congé maladie',
    [LeaveType.PERSONAL]: 'Congé personnel',
    [LeaveType.MATERNITY]: 'Congé maternité',
    [LeaveType.PATERNITY]: 'Congé paternité',
    [LeaveType.BEREAVEMENT]: 'Congé de deuil'
  };

  constructor(
    private fb: FormBuilder,
    private leaveService: LeaveService,
    private snackBar: MatSnackBar,
    private datePipe: DatePipe
  ) {
    this.rangeForm = this.fb.group({
      start: ['', Validators.required],
      end: ['', Validators.required]
    });

    this.leaveForm = this.fb.group({
      range: this.rangeForm,
      type: ['', Validators.required],
      reason: ['', [Validators.required, Validators.minLength(20)]]
    });
  }

  getLeaveTypeLabel(type: string): string {
    return this.leaveTypeLabels[type as LeaveType] || type;
  }
  calculateTotalDays(): number {
    if (this.rangeForm.valid) {
      const start = new Date(this.rangeForm.value.start);
      const end = new Date(this.rangeForm.value.end);
      const diff = end.getTime() - start.getTime();
      return Math.ceil(diff / (1000 * 3600 * 24)) + 1;
    }
    return 0;
  }

  onSubmit() {
    if (this.leaveForm.valid) {
      this.isLoading = true;
      const formValue = this.leaveForm.value;

      const dto: LeaveRequestDto = {
        startDate: this.datePipe.transform(formValue.range.start, 'yyyy-MM-dd')!,
        endDate: this.datePipe.transform(formValue.range.end, 'yyyy-MM-dd')!,
        type: formValue.type,
        reason: formValue.reason
      };

      this.leaveService.requestLeave(dto).subscribe({
        next: () => {
          this.leaveForm.reset();
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
    }
  }
}
