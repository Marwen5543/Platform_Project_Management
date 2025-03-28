import { Component, OnInit } from '@angular/core';
import { KeycloakService } from 'src/app/Service/KeycloakService';
import { UserDTO } from 'src/app/Models/user.models';
import { SidebarService } from 'src/app/Service/sidebar.service';

@Component({
  selector: 'app-acceuil',
  templateUrl: './acceuil.component.html',
  styleUrls: ['./acceuil.component.css']
})
export class AcceuilComponent implements OnInit {
  profileDropdownOpen = false;
  roles: string[] = ['Admin', 'User', 'Employee','HR'];
  user: UserDTO = {
    userId: '',
    username: '',
    email: '',
    role: 'EMPLOYEE',
    status: 'ACTIVE'
  };

  constructor(
    private keycloakService: KeycloakService,
    public sidebarService: SidebarService
  ) {}

  ngOnInit(): void {
    this.loadUserData();
  }

  private loadUserData(): void {
    if (this.keycloakService.isAuthenticated()) {
      this.user = {
        userId: this.keycloakService.getUserId() || '0',
        username: this.keycloakService.getUsername(),
        email: this.keycloakService.getEmail(),
        role: this.keycloakService.getRole(),
        status: 'ACTIVE'
      };
    }
  }

  getInitials(username?: string): string {
    if (!username) return '??';
    return username.split(' ')
      .map(part => part[0])
      .join('')
      .substring(0, 2)
      .toUpperCase();
  }

  toggleProfileDropdown(): void {
    this.profileDropdownOpen = !this.profileDropdownOpen;
  }

  logout(): void {
    this.keycloakService.logout();
  }

  getCurrentTime(): string {
    return new Date().toLocaleTimeString('fr-FR', {
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}