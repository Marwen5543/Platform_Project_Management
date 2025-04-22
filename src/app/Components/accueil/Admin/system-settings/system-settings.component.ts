import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatOptionModule } from '@angular/material/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HeaderComponent } from '../../header/header.component'; // Import HeaderComponent
import { KeycloakService } from 'src/app/Service/KeycloakService'; // Import KeycloakService
import { UserDTO, UserRole, UserStatus } from 'src/app/Models/user.models'; // Import UserDTO and related enums

@Component({
  selector: 'app-system-settings',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatSlideToggleModule,
    MatFormFieldModule,
    MatSelectModule,
    MatOptionModule,
    MatIconModule,
    MatButtonModule,
    HeaderComponent // Add HeaderComponent to imports
  ],
  templateUrl: './system-settings.component.html',
  styleUrls: ['./system-settings.component.css']
})
export class SystemSettingsComponent implements OnInit {
  // User property for the header
  user: UserDTO;

  // Display Settings
  isDarkMode = false;
  availableThemes = [
    { value: 'default', name: 'Default', color: '#d32f2f' },
    { value: 'blue', name: 'Ocean Blue', color: '#1976d2' },
    { value: 'green', name: 'Forest Green', color: '#388e3c' },
    { value: 'purple', name: 'Royal Purple', color: '#7b1fa2' }
  ];
  selectedTheme = 'default';
  fontSize = 14;
  fontSizes = [12, 13, 14, 15, 16];
  animationsEnabled = true;

  // Accessibility Settings
  highContrastMode = false;
  screenReaderSupport = false;

  // Language & Regional
  availableLanguages = [
    { code: 'en', name: 'English', nativeName: 'English' },
    { code: 'fr', name: 'French', nativeName: 'Français' },
    { code: 'es', name: 'Spanish', nativeName: 'Español' },
    { code: 'de', name: 'German', nativeName: 'Deutsch' }
  ];
  selectedLanguage = 'en';
  dateFormat = 'MM/dd/yyyy';
  timeFormat = '12';
  timeZones = [
    { value: 'UTC', label: 'UTC (Coordinated Universal Time)' },
    { value: 'PST', label: 'PST (Pacific Time)' },
    { value: 'EST', label: 'EST (Eastern Time)' },
    { value: 'CET', label: 'CET (Central European Time)' },
    { value: 'IST', label: 'IST (India Standard Time)' }
  ];
  timeZone = 'UTC';

  // Notification Settings
  emailNotifications = true;
  pushNotifications = true;
  soundEnabled = true;
  notificationFrequency = 'instant';

  // Performance Settings
  lowDataMode = false;
  backgroundSync = true;

  // Privacy & Security
  analyticsEnabled = false;
  personalizedAds = false;
  locationServices = false;
  twoFactorAuth = false;
  sessionTimeout = 30; // in minutes
  sessionTimeouts = [15, 30, 60, 120];

  // Backup & Sync
  autoBackup = false;
  syncAcrossDevices = false;

  constructor(
    private snackBar: MatSnackBar,
    private keycloakService: KeycloakService // Inject KeycloakService
  ) {
    // Initialize the user property
    this.user = this.createEmptyUserForHeader();
  }

  ngOnInit(): void {
    this.loadSettings();
    this.applyTheme();
  }

  // Create a default user object for the header
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
      hireDate: '',
      departmentId: 0,
      managerId: 0
    };
  }

  // Convert role string to UserRole enum
  private convertToUserRole(roleString: string): UserRole {
    switch (roleString?.toUpperCase()) {
      case 'SUPER_ADMIN': return UserRole.SUPER_ADMIN;
      case 'ADMIN': return UserRole.ADMIN;
      case 'MANAGER': return UserRole.MANAGER;
      case 'HR': return UserRole.HR;
      case 'EMPLOYEE': return UserRole.EMPLOYEE;
      default: return UserRole.EMPLOYEE;
    }
  }

  // Load settings from localStorage
  private loadSettings(): void {
    const settings = localStorage.getItem('systemSettings');
    if (settings) {
      const parsedSettings = JSON.parse(settings);
      Object.assign(this, parsedSettings);
    }
    this.applyTheme();
  }

  // Apply theme and dark mode using CSS variables
  public applyTheme(): void {
    const theme = this.availableThemes.find(t => t.value === this.selectedTheme);
    if (theme) {
      document.documentElement.style.setProperty('--primary-color', theme.color);
    }
    if (this.isDarkMode) {
      document.documentElement.classList.add('dark-mode');
    } else {
      document.documentElement.classList.remove('dark-mode');
    }
    // Apply font size
    document.documentElement.style.setProperty('--font-size', `${this.fontSize}px`);
  }

  // Save settings to localStorage
  saveSettings(): void {
    const settings = {
      isDarkMode: this.isDarkMode,
      selectedTheme: this.selectedTheme,
      fontSize: this.fontSize,
      animationsEnabled: this.animationsEnabled,
      highContrastMode: this.highContrastMode,
      screenReaderSupport: this.screenReaderSupport,
      selectedLanguage: this.selectedLanguage,
      dateFormat: this.dateFormat,
      timeFormat: this.timeFormat,
      timeZone: this.timeZone,
      emailNotifications: this.emailNotifications,
      pushNotifications: this.pushNotifications,
      soundEnabled: this.soundEnabled,
      notificationFrequency: this.notificationFrequency,
      lowDataMode: this.lowDataMode,
      backgroundSync: this.backgroundSync,
      analyticsEnabled: this.analyticsEnabled,
      personalizedAds: this.personalizedAds,
      locationServices: this.locationServices,
      twoFactorAuth: this.twoFactorAuth,
      sessionTimeout: this.sessionTimeout,
      autoBackup: this.autoBackup,
      syncAcrossDevices: this.syncAcrossDevices
    };
    localStorage.setItem('systemSettings', JSON.stringify(settings));
    this.applyTheme();
    this.snackBar.open('Settings saved successfully!', 'Close', { duration: 3000 });
  }

  // Reset to default values
  resetDefaults(): void {
    this.isDarkMode = false;
    this.selectedTheme = 'default';
    this.fontSize = 14;
    this.animationsEnabled = true;
    this.highContrastMode = false;
    this.screenReaderSupport = false;
    this.selectedLanguage = 'en';
    this.dateFormat = 'MM/dd/yyyy';
    this.timeFormat = '12';
    this.timeZone = 'UTC';
    this.emailNotifications = true;
    this.pushNotifications = true;
    this.soundEnabled = true;
    this.notificationFrequency = 'instant';
    this.lowDataMode = false;
    this.backgroundSync = true;
    this.analyticsEnabled = false;
    this.personalizedAds = false;
    this.locationServices = false;
    this.twoFactorAuth = false;
    this.sessionTimeout = 30;
    this.autoBackup = false;
    this.syncAcrossDevices = false;
    this.applyTheme();
    this.snackBar.open('Settings reset to defaults.', 'Close', { duration: 3000 });
  }
}