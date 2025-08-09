import { Component, OnInit, ViewChild } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { UserDTO } from 'src/app/Models/user.models';
import { UserService } from 'src/app/Service/UserService';
import { KeycloakService } from 'src/app/Service/KeycloakService';
import { FormsModule, NgForm } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faUser,
  faEnvelope,
  faPhone,
  faMapMarkerAlt,
  faBriefcase,
  faLock,
  faEye,
  faEyeSlash,
  faUserEdit,
  faSpinner,
  faSave,
  faTimes
} from '@fortawesome/free-solid-svg-icons';
import { IconProp } from '@fortawesome/fontawesome-svg-core';

@Component({
  selector: 'app-edit-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, FontAwesomeModule],
  templateUrl: './edit-profile.component.html',
  styleUrls: ['./edit-profile.component.css']
})
export class EditProfileComponent implements OnInit {
  user: UserDTO | null = null;
  isLoading = true;
  isSaving = false;
  passwordData = {
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  };
  showPasswordFields = false;

  // Password visibility toggles
  showCurrentPassword: boolean = false;
  showNewPassword: boolean = false;
  showConfirmPassword: boolean = false;

  @ViewChild('editForm') editForm!: NgForm;

  // FontAwesome icons
  faUser: IconProp = faUser as IconProp;
  faEnvelope: IconProp = faEnvelope as IconProp;
  faPhone: IconProp = faPhone as IconProp;
  faMapMarkerAlt: IconProp = faMapMarkerAlt as IconProp;
  faBriefcase: IconProp = faBriefcase as IconProp;
  faLock: IconProp = faLock as IconProp;
  faEye: IconProp = faEye as IconProp;
  faEyeSlash: IconProp = faEyeSlash as IconProp;
  faUserEdit: IconProp = faUserEdit as IconProp;
  faSpinner: IconProp = faSpinner as IconProp;
  faSave: IconProp = faSave as IconProp;
  faTimes: IconProp = faTimes as IconProp;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private userService: UserService,
    private keycloakService: KeycloakService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    const userId = this.keycloakService.getUserId();
    if (!userId) {
      this.snackBar.open('Please log in to edit your profile', 'Close', { duration: 3000 });
      this.router.navigate(['/login']);
      return;
    }
    this.loadUserDetails(userId);
  }

  loadUserDetails(userId: string): void {
    this.isLoading = true;
    this.userService.getUserDetailsById(userId).subscribe({
      next: (user: UserDTO) => {
        this.user = { ...user };
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading user details:', err);
        this.snackBar.open('Failed to load user details', 'Close', { duration: 3000 });
        this.isLoading = false;
        this.router.navigate(['/profile']);
      }
    });
  }

  togglePasswordFields(): void {
    this.showPasswordFields = !this.showPasswordFields;
    if (!this.showPasswordFields) {
      this.passwordData = { currentPassword: '', newPassword: '', confirmPassword: '' };
    }
  }

  saveProfile(): void {
    if (!this.user || this.isSaving || this.editForm.invalid) return;

    this.isSaving = true;
    const userId = this.keycloakService.getUserId();
    this.userService.updateProfile(userId, this.user).subscribe({
      next: (updatedUser: UserDTO) => {
        this.user = updatedUser;
        if (this.showPasswordFields && this.passwordData.newPassword) {
          this.updatePassword();
        } else {
          this.handleSaveSuccess();
        }
      },
      error: (err) => this.handleSaveError(err)
    });
  }

  private updatePassword(): void {
    if (this.passwordData.newPassword !== this.passwordData.confirmPassword) {
      this.snackBar.open('New password and confirmation do not match', 'Close', { duration: 3000 });
      this.isSaving = false;
      return;
    }

    this.userService.changePassword(
      this.user!.userId,
      this.passwordData.currentPassword,
      this.passwordData.newPassword
    ).subscribe({
      next: () => this.handleSaveSuccess(),
      error: (err) => this.handleSaveError(err, true)
    });
  }

  private handleSaveSuccess(): void {
    this.isSaving = false;
    this.snackBar.open('Profile updated successfully', 'Close', { duration: 3000 });
    this.router.navigate(['/profile']);
  }

  private handleSaveError(err: any, isPasswordError = false): void {
    console.error('Error updating profile:', err);
    this.isSaving = false;
    const message = isPasswordError
      ? 'Failed to update password. Current password might be incorrect.'
      : 'Failed to update profile';
    this.snackBar.open(message, 'Close', { duration: 3000 });
  }

  cancel(): void {
    this.router.navigate(['/profile']);
  }

  togglePasswordVisibility(field: string): void {
    switch (field) {
      case 'current':
        this.showCurrentPassword = !this.showCurrentPassword;
        break;
      case 'new':
        this.showNewPassword = !this.showNewPassword;
        break;
      case 'confirm':
        this.showConfirmPassword = !this.showConfirmPassword;
        break;
      default:
        break;
    }
  }
}