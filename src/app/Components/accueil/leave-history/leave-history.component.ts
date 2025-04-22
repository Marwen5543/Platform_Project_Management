import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { 
  MatCell, MatCellDef, MatColumnDef, MatHeaderCell, 
  MatHeaderCellDef, MatHeaderRowDef, MatRowDef, 
  MatTableDataSource, MatTableModule 
} from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subscription } from 'rxjs';
import { LeaveRequest, LeaveRequestDto, LeaveStatus } from 'src/app/Models/LeaveRequest';
import { LeaveService } from 'src/app/Service/LeaveService';
import { UserService } from 'src/app/Service/UserService';
import { KeycloakService } from 'src/app/Service/KeycloakService';

@Component({
  standalone: true,
  selector: 'app-leave-history',
  templateUrl: './leave-history.component.html',
  styleUrls: ['./leave-history.component.css'],
  imports: [
    CommonModule, 
    MatTableModule, 
    MatHeaderCellDef, 
    MatCellDef, 
    MatHeaderRowDef, 
    MatRowDef, 
    MatColumnDef, 
    MatHeaderCell, 
    MatCell,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule
  ],
  providers: [DatePipe] 
})

export class LeaveHistoryComponent implements OnInit, OnDestroy {
  displayedColumns: string[] = ['startDate', 'endDate', 'type', 'status', 'reason', 'actions'];
  dataSource = new MatTableDataSource<LeaveRequest>([]);
  private subscription: Subscription = new Subscription();
  isHR: boolean = true; // Set this based on user role from your auth service

  constructor(
    private leaveService: LeaveService,
    private snackBar: MatSnackBar,
    private datePipe: DatePipe,
    private userService: UserService, private keycloakService: KeycloakService
  ) {
    this.userService.getCurrentUserRoles().subscribe({
      next: roles => console.log('Backend roles:', roles),
      error: err => console.error('Error fetching backend roles:', err)
  });
  console.log('Keycloak roles:', this.keycloakService.getRoles());
  }

  ngOnInit(): void {
    this.loadLeaveHistory();
  const roles = this.keycloakService.getRoles(); 
  this.isHR = roles.includes('HR');
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  loadLeaveHistory(): void {
    this.subscription.add(
      this.leaveService.getLeaveHistory().subscribe({
        next: (leaves: LeaveRequest[]) => {
          this.dataSource.data = leaves;
        },
        error: (err) => {
          console.error('Error fetching leave history:', err);
          this.snackBar.open('Failed to load leave history', 'Close', { duration: 3000 });
        }
      })
    );
  }

  formatDate(date: string | Date): string {
    return this.datePipe.transform(date, 'dd/MM/yyyy') || '';
  }

  approveLeave(leaveId: string): void {
    this.subscription.add(
      this.leaveService.updateLeaveStatus(leaveId, LeaveStatus.APPROVED).subscribe({
        next: () => {
          this.snackBar.open('Leave request approved successfully', 'Close', { duration: 3000 });
          this.loadLeaveHistory(); // Reload data after approval
        },
        error: (err) => {
          console.error('Error approving leave request:', err);
          this.snackBar.open('Failed to approve leave request', 'Close', { duration: 3000 });
        }
      })
    );
  }

  rejectLeave(leaveId: string): void {
    // Using the existing updateLeaveStatus method with only 2 parameters
    this.subscription.add(
      this.leaveService.updateLeaveStatus(leaveId, LeaveStatus.REJECTED).subscribe({
        next: () => {
          this.snackBar.open('Leave request rejected', 'Close', { duration: 3000 });
          this.loadLeaveHistory(); // Reload data after rejection
        },
        error: (err) => {
          console.error('Error rejecting leave request:', err);
          this.snackBar.open('Failed to reject leave request', 'Close', { duration: 3000 });
        }
      })
    );
  }

  openRejectDialog(leave: LeaveRequest): void {
    // Since we can't pass a rejection reason to updateLeaveStatus,
    // we'll just confirm if the user wants to reject the request
    if (confirm('Are you sure you want to reject this leave request?')) {
      this.rejectLeave(leave.id);
    }
  }

  getStatusClass(status: string): string {
    switch (status) {
      case LeaveStatus.APPROVED:
        return 'status-approved';
      case LeaveStatus.REJECTED:
        return 'status-rejected';
      case LeaveStatus.PENDING:
        return 'status-pending';
      case LeaveStatus.CANCELLED:
        return 'status-cancelled';
      default:
        return '';
    }
  }
}