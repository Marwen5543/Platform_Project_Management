import { Component, OnInit } from '@angular/core';
import { UserDTO } from 'src/app/Models/user.models';
import { KeycloakService } from 'src/app/Service/KeycloakService';
import { SidebarService } from 'src/app/Service/sidebar.service';
import { UserService } from '../../../Service/UserService';
import { MatSnackBar } from '@angular/material/snack-bar'; 
@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent implements OnInit {
  profileDropdownOpen = false;
  user: any;
  databaseRole = '';
  isLoading = true;
  keycloakRoles: string[] = [];
  constructor(
    private keycloakService: KeycloakService,
    public sidebarService: SidebarService,
    private userService: UserService,  // Injection du service ici
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadUserData();
  }


  private loadUserData(): void {
    if (!this.keycloakService.isAuthenticated()) {
      console.warn('User not authenticated, redirecting to login');
      this.keycloakService.login();
      return;
    }
  
    this.isLoading = true;
  
    // Set basic user info from Keycloak first
    this.user = {
      userId: this.keycloakService.getUserId() || Date.now().toString(),
      username: this.keycloakService.getUsername(),
      email: this.keycloakService.getEmail(),
      status: 'ACTIVE',
      isOnline: navigator.onLine,
      emailVerified: false,
      twoFactorEnabled: false,
      joinDate: new Date().toISOString(),
      role: this.keycloakService.getRole() || 'Loading...' // Use Keycloak role initially
    };
  
    // Get database role with error handling
    this.userService.getDatabaseRole().subscribe({
      next: (response) => {
        this.databaseRole = response.databaseRole;
        this.user.role = this.databaseRole;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Role fetch error details:', {
          status: err.status,
          message: err.error,
          url: err.url
        });
        // Fall back to Keycloak role if database role fetch fails
        this.databaseRole = this.keycloakService.getRole() || 'Unknown';
        this.user.role = this.databaseRole;
        this.isLoading = false;
        
        this.snackBar.open('Could not retrieve database role, using Keycloak role instead', 'Dismiss', {
          duration: 5000
        });
      }
    });
  }

  getInitials(username?: string): string {
    if (!username) return '??';
    return username.split(' ')
      .map(part => part[0])
      .join('')
      .substring(0, 2)
      .toUpperCase();
  }
}
