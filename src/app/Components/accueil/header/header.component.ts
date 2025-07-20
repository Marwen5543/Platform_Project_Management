import { Component, Input, OnInit, OnDestroy, HostListener, ElementRef } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { interval, Subscription } from 'rxjs';
import { startWith, switchMap } from 'rxjs/operators';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { UserDTO, UserRole } from 'src/app/Models/user.models';
import { KeycloakService } from 'src/app/Service/KeycloakService';
import { DocumentService } from 'src/app/Service/document.service';
import { LeaveService } from 'src/app/Service/LeaveService';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FontAwesomeModule,
    MatSnackBarModule,
  ],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css'],
})
export class HeaderComponent implements OnInit, OnDestroy {
  @Input() user: UserDTO | any = { username: 'User', role: UserRole.EMPLOYEE };
  isMenuOpen = false;
  isProfileOpen = false;
  isCongesDropdownOpen = false;
  isDocumentsDropdownOpen = false;
  isDocManagementDropdownOpen = false;
  isAdminDropdownOpen = false;
  userRoleEnum = UserRole;
  readyDocumentsCount = 0;
  pendingTeamLeavesCount = 0;
  private documentCheckSubscription?: Subscription;
  private leaveCheckSubscription?: Subscription;

  constructor(
    private router: Router,
    private keycloakService: KeycloakService,
    private documentService: DocumentService,
    private leaveService: LeaveService,
    private snackBar: MatSnackBar,
    private elementRef: ElementRef
  ) {}

  async ngOnInit(): Promise<void> {
    console.log('HeaderComponent initialized');
    await this.initializeUserData();
    this.startDocumentCheck();
    this.startLeaveCheck();
  }

  ngOnDestroy(): void {
    console.log('HeaderComponent destroyed');
    this.stopDocumentCheck();
    this.stopLeaveCheck();
  }

  // Close profile dropdown on outside click
  @HostListener('document:click', ['$event'])
  @HostListener('document:touchstart', ['$event'])
  onDocumentClick(event: Event) {
    const target = event.target as HTMLElement;
    const clickedInsideUserInfo = this.elementRef.nativeElement.querySelector('.user-info')?.contains(target);
    const clickedInsideProfileDropdown = this.elementRef.nativeElement.querySelector('.profile-dropdown')?.contains(target);

    if (!clickedInsideUserInfo && !clickedInsideProfileDropdown && this.isProfileOpen) {
      console.log('Closing profile dropdown due to outside click/touch');
      this.isProfileOpen = false;
    }
  }

  private startDocumentCheck(): void {
    if (this.canRequestAttestations()) {
      this.documentCheckSubscription = interval(300000)
        .pipe(
          startWith(0),
          switchMap(() => this.documentService.getDocumentHistory())
        )
        .subscribe({
          next: (documents) => {
            const newReadyCount = documents.filter(
              (doc) => doc.status === 'APPROVED' || doc.status === 'GENERATED'
            ).length;
            if (newReadyCount !== this.readyDocumentsCount) {
              const newDocumentsCount = newReadyCount - this.readyDocumentsCount;
              if (newDocumentsCount > 0) {
                this.showNotification(newDocumentsCount, 'document');
              }
              this.readyDocumentsCount = newReadyCount;
              console.log('Document check updated, readyDocumentsCount:', this.readyDocumentsCount);
            }
          },
          error: (err) => console.error('Error checking documents:', err),
        });
    }
  }

  private startLeaveCheck(): void {
    if (this.hasManagerRole()) {
      this.leaveCheckSubscription = interval(300000)
        .pipe(
          startWith(0),
          switchMap(() => this.leaveService.getTeamLeaves())
        )
        .subscribe({
          next: (leaves) => {
            const newPendingCount = leaves.filter((leave) => leave.status === 'PENDING').length;
            if (newPendingCount !== this.pendingTeamLeavesCount) {
              const newLeavesCount = newPendingCount - this.pendingTeamLeavesCount;
              if (newLeavesCount > 0) {
                this.showNotification(newLeavesCount, 'leave');
              }
              this.pendingTeamLeavesCount = newPendingCount;
              console.log('Leave check updated, pendingTeamLeavesCount:', this.pendingTeamLeavesCount);
            }
          },
          error: (err) => console.error('Error checking team leaves:', err),
        });
    }
  }

  private showNotification(count: number, type: 'document' | 'leave'): void {
    const message =
      type === 'document'
        ? `You have ${count} new document(s) ready for download`
        : `You have ${count} new pending team leave(s) to review`;
    const action = type === 'document' ? 'View Documents' : 'View Leaves';
    console.log('Showing notification:', message);
    this.snackBar.open(message, action, {
      duration: 5000,
      panelClass: ['mat-snack-bar'],
      verticalPosition: 'bottom',
      horizontalPosition: 'center',
    }).onAction().subscribe(() => {
      this.router.navigate([type === 'document' ? '/document-history' : '/liste-equipe-conge']);
    });
  }

  private stopDocumentCheck(): void {
    if (this.documentCheckSubscription) {
      this.documentCheckSubscription.unsubscribe();
    }
  }

  private stopLeaveCheck(): void {
    if (this.leaveCheckSubscription) {
      this.leaveCheckSubscription.unsubscribe();
    }
  }

  private async initializeUserData(): Promise<void> {
    if (this.keycloakService.isAuthenticated()) {
      const username = this.keycloakService.getUsername();
      const roleString = this.keycloakService.getRole();
      const role = this.convertToUserRole(roleString);
      this.user = {
        ...this.user,
        username: username || 'Unknown User',
        role: role,
      };
      console.log('Header user initialized:', this.user);
    }
  }

  private convertToUserRole(roleString: string): UserRole {
    if (!roleString) return UserRole.EMPLOYEE;
    switch (roleString.toUpperCase()) {
      case 'SUPER_ADMIN': return UserRole.SUPER_ADMIN;
      case 'ADMIN': return UserRole.ADMIN;
      case 'MANAGER': return UserRole.MANAGER;
      case 'HR': return UserRole.HR;
      case 'EMPLOYEE': return UserRole.EMPLOYEE;
      default: return UserRole.EMPLOYEE;
    }
  }

  toggleMenu(): void {
    console.log('Toggling menu, isMenuOpen:', this.isMenuOpen);
    this.isMenuOpen = !this.isMenuOpen;
    this.isProfileOpen = false;
    if (!this.isMenuOpen) {
      this.resetMobileDropdowns();
    }
  }

  toggleProfile(): void {
    console.log('Toggling profile, isProfileOpen:', this.isProfileOpen);
    this.isProfileOpen = !this.isProfileOpen;
    this.isMenuOpen = false;
  }

  toggleDropdown(event: Event): void {
    event.stopPropagation();
  }

  toggleMobileDropdown(dropdown: 'conges' | 'documents' | 'doc-management' | 'admin'): void {
    console.log('Toggling mobile dropdown:', dropdown);
    this.isCongesDropdownOpen = dropdown === 'conges' ? !this.isCongesDropdownOpen : false;
    this.isDocumentsDropdownOpen = dropdown === 'documents' ? !this.isDocumentsDropdownOpen : false;
    this.isDocManagementDropdownOpen =
      dropdown === 'doc-management' ? !this.isDocManagementDropdownOpen : false;
    this.isAdminDropdownOpen = dropdown === 'admin' ? !this.isAdminDropdownOpen : false;
  }

  resetMobileDropdowns(): void {
    this.isCongesDropdownOpen = false;
    this.isDocumentsDropdownOpen = false;
    this.isDocManagementDropdownOpen = false;
    this.isAdminDropdownOpen = false;
  }

  navigateTo(route: string): void {
    console.log('Navigating to:', route);
    this.isMenuOpen = false;
    this.isProfileOpen = false;
    this.resetMobileDropdowns();
    this.router.navigate([route]);
  }

  logout(): void {
    console.log('Logout called');
    try {
      this.keycloakService.logout();
      this.router.navigate(['/login']);
    } catch (err) {
      console.error('Logout error:', err);
      this.snackBar.open('Error logging out', 'Close', { duration: 3000 });
    }
  }

  hasManagerRole(): boolean {
    return [UserRole.MANAGER, UserRole.HR, UserRole.ADMIN, UserRole.SUPER_ADMIN].includes(this.user?.role);
  }

  canShowAdminLink(): boolean {
    return this.user?.role === UserRole.ADMIN;
  }

  canShowSuperAdminLinks(): boolean {
    return this.user?.role === UserRole.SUPER_ADMIN;
  }

  canShowHrLinks(): boolean {
    return this.user?.role === UserRole.HR;
  }

  canRequestAttestations(): boolean {
    return (
      [UserRole.EMPLOYEE, UserRole.MANAGER].includes(this.user?.role) &&
      ![UserRole.HR, UserRole.SUPER_ADMIN, UserRole.ADMIN].includes(this.user?.role)
    );
  }

  canAccessLeaveManagement(): boolean {
  return (
    [UserRole.EMPLOYEE, UserRole.MANAGER, UserRole.HR].includes(this.user?.role) &&
    this.user?.role !== UserRole.ADMIN &&
    this.user?.role !== UserRole.SUPER_ADMIN
  );
}

  getRoleDisplayName(role: UserRole): string {
    switch (role) {
      case UserRole.SUPER_ADMIN: return 'Super Admin';
      case UserRole.ADMIN: return 'Admin';
      case UserRole.MANAGER: return 'Manager';
      case UserRole.HR: return 'HR';
      case UserRole.EMPLOYEE: return 'Employee';
      default: return 'Unknown Role';
    }
  }
}