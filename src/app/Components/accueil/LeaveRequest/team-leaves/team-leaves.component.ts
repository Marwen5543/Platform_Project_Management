import { Component, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { LeaveRequest, LeaveStatus } from 'src/app/Models/LeaveRequest';
import { LeaveService } from 'src/app/Service/LeaveService';
import { CommonModule, DatePipe } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatSelectModule } from '@angular/material/select';
import { MatOptionModule } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { FormsModule } from '@angular/forms';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';

@Component({
  selector: 'app-team-leaves',
  standalone: true,
  imports: [
    FormsModule,
    CommonModule,
    DatePipe,
    MatTableModule,
    MatSelectModule,
    MatOptionModule,
    MatFormFieldModule
  ],
  templateUrl: './team-leaves.component.html',
  styleUrl: './team-leaves.component.css'
})
export class TeamLeavesComponent implements OnInit {
  leaves: LeaveRequest[] = [];
  displayedColumns: string[] = ['employeeId', 'startDate', 'endDate', 'type', 'status', 'reason', 'action'];
  error: string | null = null;
  leaveStatuses = Object.values(LeaveStatus);
  loading = false;

  constructor(
    private leaveService: LeaveService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit() {
    this.fetchTeamLeaves();
  }

  fetchTeamLeaves() {
    this.loading = true;
    this.error = null;
    
    this.leaveService.getTeamLeaves()
      .pipe(
        catchError(err => {
          this.error = err.message || 'Failed to fetch team leaves. Please try again later.';
          this.snackBar.open(`Error: ${this.error}`, 'Close', { duration: 5000 });
          return of([]);
        }),
        finalize(() => this.loading = false)
      )
      .subscribe(data => {
        this.leaves = data;
        if (data.length === 0 && !this.error) {
          this.snackBar.open('No team leave requests found.', 'Close', { duration: 3000 });
        }
      });
  }

  updateStatus(leave: LeaveRequest, status: LeaveStatus) {
    if (!leave.id) {
      this.snackBar.open('Cannot update leave: Missing leave ID', 'Close', { duration: 3000 });
      return;
    }
    
    this.leaveService.updateLeaveStatus(leave.id, status)
      .pipe(
        catchError(err => {
          this.error = err.message;
          this.snackBar.open(`Error updating status: ${err.message}`, 'Close', { duration: 5000 });
          return of(leave); // Return original leave to prevent UI changes on error
        })
      )
      .subscribe(updatedLeave => {
        const index = this.leaves.findIndex(l => l.id === updatedLeave.id);
        if (index !== -1) {
          this.leaves[index] = updatedLeave;
          this.snackBar.open('Status updated successfully!', 'Close', { duration: 3000 });
        }
      });
  }

  retry() {
    this.fetchTeamLeaves();
  }
}