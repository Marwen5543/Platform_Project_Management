import { Component, OnInit } from '@angular/core';
import { KeycloakService } from 'src/app/Service/KeycloakService';
import { SidebarService } from 'src/app/Service/sidebar.service';
import { UserService } from '../../../Service/UserService';
import { MatSnackBar } from '@angular/material/snack-bar';
import { UserDTO } from '../../../Models/user.models';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent implements OnInit {
  profileDropdownOpen = false;
  user: UserDTO = this.createEmptyUser();
  isLoading = true;

  constructor(
    private keycloakService: KeycloakService,
    public sidebarService: SidebarService,
    private userService: UserService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.initializeUserData();
  }

  private createEmptyUser(): UserDTO {
    return {
      userId: '0',
      username: 'Utilisateur inconnu',
      email: 'Non renseigné',
      role: 'EMPLOYEE',
      status: 'ACTIVE',
      phone: 'Chargement...',
      address: 'Chargement...',
      position: 'Chargement...',
      isOnline: navigator.onLine,
      emailVerified: false,
      twoFactorEnabled: false,
      joinDate: new Date().toISOString().split('T')[0]
    };
  }

  private initializeUserData(): void {
    if (!this.keycloakService.isAuthenticated()) {
      this.handleUnauthenticatedUser();
      return;
    }

    this.isLoading = true;
    this.initializeFromKeycloak();
    this.loadDatabaseRole();
    this.loadUserDetails();
  }

  private handleUnauthenticatedUser(): void {
    console.warn('User not authenticated, redirecting to login');
    this.keycloakService.login();
  }

  private initializeFromKeycloak(): void {
    this.user = {
      ...this.user,
      username: this.keycloakService.getUsername() || this.user.username,
      email: this.keycloakService.getEmail() || this.user.email,
      role: this.keycloakService.getRole() || this.user.role
    };
  }

  private loadDatabaseRole(): void {
    this.userService.getDatabaseRole().subscribe({
      next: (response) => {
        this.user.role = response.databaseRole || this.user.role;
      },
      error: (err) => this.handleRoleError(err)
    });
  }

  private loadUserDetails(): void {
    const username = this.keycloakService.getUsername();
    if (!username) {
      this.handleMissingUsername();
      return;
    }

    this.userService.getUserDetails(username).subscribe({
      next: (details) => this.handleUserDetails(details),
      error: (err) => this.handleDetailsError(err),
      complete: () => this.isLoading = false
    });
  }

  private handleUserDetails(details: UserDTO): void {
    this.user = {
      ...this.user,
      ...details,
      isOnline: navigator.onLine,
      joinDate: this.user.joinDate
    };
  }

  private handleRoleError(err: any): void {
    console.error('Error fetching role:', err);
    this.snackBar.open('Error loading user role', 'Close', { duration: 3000 });
    this.isLoading = false;
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
      case 404: return 'User not found in database';
      case 400: return 'Invalid request format';
      default: return 'Error loading user details';
    }
  }

 // profile.component.ts
getInitials(username?: string): string {
  if (!username) return '??';
  
  return username
    .split(/\s+/)
    .map(part => part[0]?.toUpperCase() ?? '')
    .join('')
    .substring(0, 2) || '??';
}
}