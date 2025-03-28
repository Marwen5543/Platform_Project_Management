import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { UserDTO } from 'src/app/Models/user.models';
import { UserService } from 'src/app/Service/UserService';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { trigger, state, style, animate, transition } from '@angular/animations';
import { UserRole } from 'src/app/Models/User';

@Component({
  selector: 'app-manage-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
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

  constructor(
    private userService: UserService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.userService.getAllUsers().subscribe(
      (data) => {
        this.users = [...data];
        this.expandedRows = new Array(this.users.length).fill(false);
        this.errorMessage = null;
        console.log('Users loaded:', this.users); // Debug log
      },
      (error) => {
        console.error('Error fetching users:', error);
        this.errorMessage = 'Failed to load users. Please try again.';
      }
    );
  }

  changeUserRole(userId: string, newRole: string): void {
    this.errorMessage = null;

    // Find user and perform optimistic update
    const userIndex = this.users.findIndex((u) => u.userId === userId);
    let originalRole: string | undefined;

    if (userIndex !== -1) {
      originalRole = this.users[userIndex].role;
      this.users[userIndex].role = newRole;
      // Directly update role without changing the reference
      console.log('Optimistic update applied:', this.users[userIndex]); // Debug log
    }

    this.userService.changeUserRole(userId, newRole).subscribe({
      next: () => {
        console.log(`Role for user ${userId} changed to ${newRole} on backend`);
      },
      error: (error) => {
        console.error('Error changing user role:', error);
        const errorMsg = error.error?.message || error.message || 'An unknown error occurred';
        this.errorMessage = `Failed to change role: ${errorMsg}`;

        // Rollback on failure
        if (userIndex !== -1 && originalRole !== undefined) {
          this.users[userIndex].role = originalRole;
          console.log('Rolled back to:', this.users[userIndex]); // Debug log
        }
      },
      complete: () => {
        console.log('Role change request completed');
      }
    });
  }

  toggleDetails(index: number): void {
    this.expandedRows[index] = !this.expandedRows[index];
  }
}
