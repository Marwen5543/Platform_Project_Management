import { Component, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { UserDTO, UserRole, UserStatus } from '../../../Models/user.models';
import { Router } from '@angular/router';
import { KeycloakService } from 'src/app/Service/KeycloakService';
import { UserService } from 'src/app/Service/UserService';
import { CommonModule } from '@angular/common';
import { HeaderComponent } from '../header/header.component';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    HeaderComponent
  ],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent implements OnInit {

  profileDropdownOpen = false;
  user: UserDTO | null = null;
  isLoading = true;
  isUpdating = false;

  constructor(
    private keycloakService: KeycloakService,
    private userService: UserService,
    private snackBar: MatSnackBar,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initializeUserData();
  }

  private createEmptyUser(): UserDTO {
    return {
      userId: '0',
      username: 'Utilisateur inconnu',
      email: 'Non renseigné',
      role: UserRole.EMPLOYEE, // Default to employee role
      status: UserStatus.INACTIVE,
      phone: 'Chargement...',
      address: 'Chargement...',
      position: 'Chargement...',
      isOnline: false,
      emailVerified: false,
      twoFactorEnabled: false
    };
  }

  private async initializeUserData(): Promise<void> {
    if (!this.keycloakService.isAuthenticated()) {
      await this.handleUnauthenticatedUser();
      return;
    }

    this.isLoading = true;
    
    // Create a mock user for demo purposes - ensures we have something to work with
    this.user = this.createEmptyUser();
    
    
    // For demo/testing - assign admin role directly if needed
    // this.user.role = UserRole.ADMIN; // Uncomment this line for testing admin role
    
    // Normal flow
    await this.initializeFromKeycloak();
    this.loadUserDetails();
  }

  private async initializeFromKeycloak(): Promise<void> {
    if (!this.user) {
      this.user = this.createEmptyUser();
    }
    
    // For debugging - log role as defined in keycloak
    const roleString = this.keycloakService.getRole();
    console.log('Role from Keycloak:', roleString);
    
    // Determine role enum value (important for admin check)
    const roleEnum = this.convertToUserRole(roleString);
    console.log('Converted role enum:', roleEnum);
    
    const username = this.keycloakService.getUsername();
    const email = this.keycloakService.getEmail();

    // Ensure the user object is updated properly
    if (this.user) {
      this.user.username = username || 'Utilisateur inconnu';
      this.user.email = email || 'Non renseigné';
      this.user.role = roleEnum;
      
      // Log the updated user for debugging
      console.log('Updated user after Keycloak init:', this.user);
    }
  }

  private async handleUnauthenticatedUser(): Promise<void> {
    console.warn('User not authenticated, redirecting to login');
    try {
      await this.keycloakService.login();
    } catch (error) {
      console.error('Login failed:', error);
      this.snackBar.open('Failed to redirect to login', 'Close', { duration: 3000 });
    }
  }

  private convertToUserRole(roleString: string): UserRole {
    // Map keycloak roles to UserRole enum values
    console.log('Converting role string:', roleString);
    
    if (!roleString) return UserRole.EMPLOYEE;
    
    switch (roleString.toUpperCase()) {
      case 'SUPER_ADMIN': 
        console.log('Converting to SUPER_ADMIN');
        return UserRole.SUPER_ADMIN;
      case 'ADMIN': 
        console.log('Converting to ADMIN');
        return UserRole.ADMIN;
      case 'MANAGER': return UserRole.MANAGER;
      case 'HR': return UserRole.HR;
      case 'DEFAULT-ROLES-DEMO': return UserRole.EMPLOYEE;
      case 'EMPLOYEE': return UserRole.EMPLOYEE;
      default: return UserRole.EMPLOYEE;
    }
  }

  private loadUserDetails(): void {
    const username = this.keycloakService.getUsername();
    if (!username) {
      this.handleMissingUsername();
      return;
    }

    this.userService.getUserDetails(username).subscribe({
      next: (details) => {
        this.handleUserDetails(details);
        this.isLoading = false;
      },
      error: (err) => this.handleDetailsError(err),
      complete: () => {}
    });
  }

  private handleUserDetails(details: UserDTO): void {
    console.log('Received user details:', details);
    
    if (this.user) {
      // Preserve the role from Keycloak if details don't have one
      const role = details.role !== undefined ? details.role : this.user.role;
      
      this.user = {
        ...this.user,
        ...details,
        role // Ensure role is preserved
      };
      
      console.log('Updated user after details:', this.user);
    } else {
      this.user = details;
    }
  }

  private handleDetailsError(err: any): void {
    console.error('Error fetching user details:', err);
    const errorMessage = this.getErrorMessage(err.status);
    this.snackBar.open(errorMessage, 'Close', { duration: 3000 });
    this.isLoading = false;
  }

  private handleMissingUsername(): void {
    console.warn('Username not available');
    this.snackBar.open('Cannot load user details', 'Close', { duration: 3000 });
    this.isLoading = false;
  }

  private getErrorMessage(status: number): string {
    switch (status) {
      case 404:
        return 'User not found in database';
      case 400:
        return 'Invalid request format';
      default:
        return 'Error loading user details';
    }
  }

  getInitials(username?: string): string {
    if (!username) return '??';
    return (
      username
        .split(/\s+/)
        .map((part) => part[0]?.toUpperCase() ?? '')
        .join('')
        .substring(0, 2) || '??'
    );
  }

  getRoleDisplayName(role: UserRole | null | undefined): string { 
    if (role === undefined || role === null) {
        return 'Unknown Role';
    }

    switch (role) {
        case UserRole.SUPER_ADMIN: return 'Super Admin';
        case UserRole.ADMIN: return 'Admin'; 
        case UserRole.MANAGER: return 'Manager';
        case UserRole.HR: return 'HR';
        case UserRole.EMPLOYEE: return 'Employee';
        default: return 'Employee';
    }
}

  manageSecurity(): void {
    this.router.navigate(['/security-settings']);
  }


  updateProfile(): void {
    if (!this.user || !this.user.userId) {
      this.snackBar.open('Unable to edit profile', 'Close', { duration: 3000 });
      return;
    }
  
    // Navigate to edit profile route with UUID
    this.router.navigate(['/edit-profile', this.user.userId]);
  }

}