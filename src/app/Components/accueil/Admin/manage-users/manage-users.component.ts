import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { UserDTO, UserRole, UserStatus } from 'src/app/Models/user.models';
import { UserService } from 'src/app/Service/UserService';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { trigger, state, style, animate, transition } from '@angular/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { KeycloakService } from 'src/app/Service/KeycloakService';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HeaderComponent } from '../../header/header.component';

@Component({
  selector: 'app-manage-users',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatProgressSpinnerModule,
    HeaderComponent
  ],
  templateUrl: './manage-users.component.html',
  styleUrls: ['./manage-users.component.css'],
  animations: [
    trigger('slideDown', [
      state('void', style({ height: '0px', opacity: 0 })),
      state('*', style({ height: '*', opacity: 1 })),
      transition('void <=> *', animate('300ms ease-in-out')),
    ]),
  ],
})
export class ManageUsersComponent implements OnInit {
  users: UserDTO[] = [];
  userRoles = Object.values(UserRole);
  expandedRows: boolean[] = [];
  errorMessage: string | null = null;
  isLoading: boolean = true;
  UserRole = UserRole;
  currentUserRole: UserRole;
  currentUserId: string | null = null;
  isChangingRole: { [userId: string]: boolean } = {};
  isDeletingUser: { [userId: string]: boolean } = {};

  user: UserDTO = this.createEmptyUserForHeader();

  constructor(
    private userService: UserService,
    private cdr: ChangeDetectorRef,
    private snackBar: MatSnackBar,
    private keycloakService: KeycloakService
  ) {
    this.currentUserRole = this.convertToUserRole(this.keycloakService.getRole());
  }

  ngOnInit(): void {
    console.log('Raw role from Keycloak:', this.keycloakService.getRole());
    console.log('All roles from Keycloak:', this.keycloakService.getRoles());
    this.loadUserDetails();
    this.initializeHeaderUser();
    this.currentUserId = this.keycloakService.getUserId();
  }

  private createEmptyUserForHeader(): UserDTO {
    return {
      userId: '0',
      username: this.keycloakService.getUsername() || 'Unknown User',
      email: this.keycloakService.getEmail() || 'unknown@example.com',
      role: this.convertToUserRole(this.keycloakService.getRole()) || UserRole.EMPLOYEE,
      status: UserStatus.INACTIVE,
      roles: [],
      phone: '',
      address: '',
      position: '',
      hireDate: null, // Changed from '' to null for consistency
      departmentId: 0,
      managerId: 0
    };
  }

  // Format hireDate to handle string, Date, null, or undefined
  formatHireDate(hireDate: string | Date | null | undefined): string {
    if (!hireDate) {
      return 'N/A';
    }
    const date = new Date(hireDate);
    if (isNaN(date.getTime())) {
      return 'N/A'; // Invalid date
    }
    return date.toISOString().split('T')[0]; // Returns yyyy-MM-dd
  }

  getRoleDisplayName(role: UserRole): string {
    switch (role) {
      case UserRole.SUPER_ADMIN: return 'Super Admin (System)';
      case UserRole.ADMIN: return 'Admin';
      case UserRole.MANAGER: return 'Manager';
      case UserRole.HR: return 'HR';
      case UserRole.EMPLOYEE: return 'Employee';
      default: return 'Unknown Role';
    }
  }

  private convertToUserRole(roleString: string): UserRole {
    if (!roleString) return UserRole.EMPLOYEE;
    const upperRole = roleString.toUpperCase();
    if (upperRole.includes('SUPER_ADMIN')) return UserRole.SUPER_ADMIN;
    if (upperRole.includes('ADMIN')) return UserRole.ADMIN;
    if (upperRole.includes('MANAGER')) return UserRole.MANAGER;
    if (upperRole.includes('HR')) return UserRole.HR;
    return UserRole.EMPLOYEE;
  }

  private initializeHeaderUser(): void {
    this.user = this.createEmptyUserForHeader();
  }

  private loadUserDetails(): void {
    console.log('loadUserDetails() called');
    this.isLoading = true;
    this.userService.getAllUsers().subscribe({
      next: (data) => {
        console.log('loadUserDetails() - userService.getAllUsers() next:', data);
        this.users = data;
        this.expandedRows = new Array(this.users.length).fill(false);
        this.errorMessage = null;
        this.isLoading = false;
        this.cdr.detectChanges();
        console.log('loadUserDetails() - Change detection triggered');
      },
      error: (err) => {
        console.error('loadUserDetails() - userService.getAllUsers() error:', err);
        this.errorMessage = this.getErrorMessage(err.status);
        // Snackbar message removed as per previous request
        this.isLoading = false;
      }
    });
  }

  private getErrorMessage(status: number): string {
    switch (status) {
      case 404: return 'No users found';
      case 403: return 'You are not authorized to view users';
      case 500: return 'Server error occurred while loading users';
      default: return 'Failed to load users. Please try again.';
    }
  }

  changeUserRole(userId: string, newRole: UserRole): void {
    if (newRole === UserRole.SUPER_ADMIN) {
      this.showSnackbar('Cannot assign SUPER_ADMIN role');
      return;
    }
    console.log('changeUserRole() called for userId:', userId, 'newRole:', newRole);
    const userIndex = this.users.findIndex(u => u.userId === userId);
    if (userIndex === -1 || !this.canChangeRole(this.users[userIndex])) {
      this.showSnackbar('Only SUPER_ADMIN or ADMIN can change user roles.');
      return;
    }

    this.isChangingRole[userId] = true;
    this.cdr.detectChanges();

    this.userService.changeUserRole(userId, newRole).subscribe({
      next: (response) => {
        console.log('changeUserRole() - userService.changeUserRole() next: Role changed successfully', response);
        this.showSnackbar('Role updated successfully', 2000);
        this.loadUserDetails();
        this.cdr.detectChanges();
        console.log('changeUserRole() - loadUserDetails() called and Change detection triggered');
      },
      complete: () => {
        console.log('changeUserRole() - userService.changeUserRole() complete');
        this.isChangingRole[userId] = false;
        this.cdr.detectChanges();
      }
    });
  }

  private getRoleChangeErrorMessage(error: any): string {
    if (error.error?.message) return error.error.message;
    if (error.status === 403) return 'Permission denied for role change';
    if (error.status === 404) return 'User not found';
    return 'Failed to change role';
  }

  private showSnackbar(message: string | null, duration: number = 3000): void {
    this.snackBar.open(message ?? 'An error occurred', 'Close', { duration });
  }

  isRoleAssignable(role: UserRole): boolean {
    return role !== UserRole.SUPER_ADMIN;
  }

  canChangeRole(user: UserDTO): boolean {
    if (user.role === UserRole.SUPER_ADMIN) {
      return false;
    }
    return this.currentUserRole === UserRole.SUPER_ADMIN || 
           this.currentUserRole === UserRole.ADMIN;
  }

  isRoleBeingChanged(userId: string): boolean {
    return this.isChangingRole[userId] ?? false;
  }

  toggleDetails(index: number): void {
    this.expandedRows[index] = !this.expandedRows[index];
  }

  isCurrentUser(user: UserDTO): boolean {
    return user.userId === this.currentUserId;
  }

  canDeleteUser(): boolean {
    return this.currentUserRole === UserRole.ADMIN || 
           this.currentUserRole === UserRole.SUPER_ADMIN;
  }

  deleteUser(userId: string): void {
    if (!this.canDeleteUser()) {
      this.showSnackbar('You are not authorized to delete users.');
      return;
    }

    if (this.isCurrentUserDeleting(userId)) {
      this.showSnackbar('Cannot delete currently logged-in user.');
      return;
    }

    if (confirm('Are you sure you want to delete this user? This action cannot be undone.')) {
      this.isDeletingUser[userId] = true;
      this.cdr.detectChanges();

      this.userService.deleteUser(userId).subscribe({
        next: (response) => {
          console.log('deleteUser() - userService.deleteUser() next: User deleted successfully', response);
          this.showSnackbar('User deleted successfully', 2000);
          this.loadUserDetails();
          this.cdr.detectChanges();
        },
        error: (error) => {
          console.error('deleteUser() - userService.deleteUser() error:', error);
          const message = this.getDeleteUserErrorMessage(error);
          this.showSnackbar(message);
        },
        complete: () => {
          this.isDeletingUser[userId] = false;
          this.cdr.detectChanges();
        }
      });
    }
  }

  isUserBeingDeleted(userId: string): boolean {
    return this.isDeletingUser[userId] ?? false;
  }

  isCurrentUserDeleting(userId: string): boolean {
    return this.isCurrentUser(this.users.find(user => user.userId === userId)!);
  }

  private getDeleteUserErrorMessage(error: any): string {
    if (error.error?.message) return error.error.message;
    if (error.status === 403) return 'Permission denied to delete user.';
    if (error.status === 404) return 'User not found for deletion.';
    return 'Failed to delete user. Please try again.';
  }
}