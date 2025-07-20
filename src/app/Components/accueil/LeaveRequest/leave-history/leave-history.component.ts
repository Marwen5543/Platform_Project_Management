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
import { LeaveRequest, LeaveRequestDto, LeaveStatus, LeaveType } from 'src/app/Models/LeaveRequest';
import { LeaveService } from 'src/app/Service/LeaveService';
import { KeycloakService } from 'src/app/Service/KeycloakService';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { jwtDecode } from 'jwt-decode';

interface DecodedToken {
  realm_access?: { roles: string[] };
}

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
  displayedColumns: string[] = ['username', 'startDate', 'endDate', 'type', 'status', 'reason', 'actions'];
  dataSource = new MatTableDataSource<LeaveRequest>([]);
  private subscription: Subscription = new Subscription();
  isHR: boolean = false;
  isAdmin: boolean = false;

  // French translations for leave types
  leaveTypeLabels: { [key in LeaveType]: string } = {
    [LeaveType.VACATION]: 'Congé payé',
    [LeaveType.SICK]: 'Congé maladie',
    [LeaveType.PERSONAL]: 'Congé personnel',
    [LeaveType.MATERNITY]: 'Congé maternité',
    [LeaveType.PATERNITY]: 'Congé paternité',
    [LeaveType.BEREAVEMENT]: 'Congé de deuil'
  };

  // French translations for leave statuses
  statusLabels: { [key in LeaveStatus]: string } = {
    [LeaveStatus.PENDING]: 'En attente',
    [LeaveStatus.APPROVED]: 'Approuvé',
    [LeaveStatus.REJECTED]: 'Rejeté',
    [LeaveStatus.CANCELLED]: 'Annulé'
  };

  constructor(
    private leaveService: LeaveService,
    private snackBar: MatSnackBar,
    private datePipe: DatePipe,
    private keycloakService: KeycloakService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.checkUserRole();
    this.loadLeaveHistory();
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  checkUserRole(): void {
    this.keycloakService.getToken().then(token => {
      const decoded = jwtDecode<DecodedToken>(token);
      this.isHR = decoded.realm_access?.roles.includes('HR') || false;
      this.isAdmin = decoded.realm_access?.roles.includes('ADMIN') || false;
      console.log('User roles:', decoded.realm_access?.roles);
    }).catch(error => {
      console.error('Error checking user role:', error);
    });
  }

  loadLeaveHistory(): void {
    this.subscription.add(
      this.leaveService.getLeaveHistory().subscribe({
        next: (leaves: LeaveRequest[]) => {
          console.log('Raw leave history response:', leaves);
          this.dataSource.data = leaves;
          console.log('DataSource data:', this.dataSource.data);
        },
        error: (err) => {
          console.error('Erreur lors de la récupération de l\'historique des congés :', err);
          //this.snackBar.open('Échec du chargement de l\'historique des congés', 'Fermer', { duration: 3000 });
        }
      })
    );
  }

  triggerMigration(): void {
    this.keycloakService.getToken().then(token => {
      const headers = new HttpHeaders({
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json'
      });
      this.http.post('http://localhost:8088/api/admin/migrate-usernames', {}, { headers }).subscribe({
        next: () => {
          this.snackBar.open('Migration des noms d\'utilisateur terminée avec succès', 'Fermer', {
            duration: 3000,
            panelClass: ['success-snackbar']
          });
          this.loadLeaveHistory(); // Refresh table
        },
        error: (error) => {
          console.error('Erreur lors du déclenchement de la migration :', error);
          this.snackBar.open('Échec de la migration des noms d\'utilisateur', 'Fermer', {
            duration: 5000,
            panelClass: ['error-snackbar']
          });
        }
      });
    }).catch(error => {
      console.error('Erreur lors de l\'obtention du jeton pour la migration :', error);
      this.snackBar.open('Échec de l\'authentification pour la migration', 'Fermer', {
        duration: 5000,
        panelClass: ['error-snackbar']
      });
    });
  }

  formatDate(date: string | Date): string {
    return this.datePipe.transform(date, 'dd/MM/yyyy') || '';
  }

  getLeaveTypeLabel(type: LeaveType): string {
    return this.leaveTypeLabels[type] || type;
  }

  getStatusLabel(status: LeaveStatus): string {
    return this.statusLabels[status] || status;
  }

  approveLeave(leaveId: string): void {
    this.subscription.add(
      this.leaveService.updateLeaveStatus(leaveId, LeaveStatus.APPROVED).subscribe({
        next: () => {
          this.snackBar.open('Demande de congé approuvée avec succès', 'Fermer', { duration: 3000 });
          this.loadLeaveHistory();
        },
        error: (err) => {
          console.error('Erreur lors de l\'approbation de la demande de congé :', err);
          this.snackBar.open('Échec de l\'approbation de la demande de congé', 'Fermer', { duration: 3000 });
        }
      })
    );
  }

  rejectLeave(leaveId: string): void {
    this.subscription.add(
      this.leaveService.updateLeaveStatus(leaveId, LeaveStatus.REJECTED).subscribe({
        next: () => {
          this.snackBar.open('Demande de congé rejetée', 'Fermer', { duration: 3000 });
          this.loadLeaveHistory();
        },
        error: (err) => {
          console.error('Erreur lors du rejet de la demande de congé :', err);
          this.snackBar.open('Échec du rejet de la demande de congé', 'Fermer', { duration: 3000 });
        }
      })
    );
  }

  openRejectDialog(leave: LeaveRequest): void {
    if (confirm('Êtes-vous sûr de vouloir rejeter cette demande de congé ?')) {
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