import { Component, ViewEncapsulation } from '@angular/core';
import {  Input, Output, EventEmitter } from '@angular/core';
import { Router } from '@angular/router';
import { KeycloakService } from 'src/app/Service/KeycloakService';
import { SidebarService } from 'src/app/Service/sidebar.service';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css'],
  encapsulation: ViewEncapsulation.None // Disable encapsulation

})
export class HeaderComponent {
  @Input() user: any = { username: 'User' };
  profileDropdownOpen = false;

  constructor(
    private router: Router,
    public sidebarService: SidebarService,
    private keycloakService: KeycloakService
  ) {}

  toggleProfileDropdown() {
    this.profileDropdownOpen = !this.profileDropdownOpen;
  }

  navigateToProfile() {
    this.profileDropdownOpen = false;
    this.router.navigate(['/profile']);
  }

  logout() {
    this.keycloakService.logout();
    // Then navigate
    this.sidebarService.navigateTo('/login');
  }
  }
