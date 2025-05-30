import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { UserDTO, UserRole, UserStatus } from 'src/app/Models/user.models';
import { UserService } from 'src/app/Service/UserService';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { trigger, state, style, animate, transition } from '@angular/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { KeycloakService } from 'src/app/Service/KeycloakService';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HeaderComponent } from '../../../header/header.component';
import { finalize } from 'rxjs/operators';

interface CompanyProject {
  title: string;
  description: string;
  position: string;
  subProjects?: { title: string; description: string }[];
}

@Component({
  selector: 'app-project-affectation',
  standalone: true,
  imports: [CommonModule, FormsModule, MatProgressSpinnerModule, HeaderComponent],
  templateUrl: './project-affectation.component.html',
  styleUrls: ['./project-affectation.component.css'],
  animations: [
    trigger('slideDown', [
      state('void', style({ height: '0px', opacity: 0 })),
      state('*', style({ height: '*', opacity: 1 })),
      transition('void <=> *', animate('300ms ease-in-out')),
    ]),
  ],
})
export class ProjectAffectationComponent implements OnInit {
  users: UserDTO[] = [];
  featuredProjects: CompanyProject[] = [];
  expandedRows: boolean[] = [];
  errorMessage: string | null = null;
  isLoading: boolean = true;
  UserRole = UserRole;
  currentUserRole: UserRole;
  user: UserDTO;
  isAssigningProject: { [userId: string]: boolean } = {};
  isDeassigningProject: { [userId: string]: boolean } = {};
  operationInProgress: boolean = false;

  constructor(
    private userService: UserService,
    private cdr: ChangeDetectorRef,
    private snackBar: MatSnackBar,
    private keycloakService: KeycloakService
  ) {
    this.currentUserRole = this.convertToUserRole(this.keycloakService.getRole());
    this.user = this.createEmptyUserForHeader();
  }

  ngOnInit(): void {
    this.loadUserDetails();
    this.loadFeaturedProjects();
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
      hireDate: '',
      departmentId: 0,
      managerId: 0,
      projectTitles: []
    };
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

  private loadUserDetails(): void {
    this.isLoading = true;
    console.log('Starting to load user details...');
    this.userService.getAllUsers().subscribe({
      next: (data) => {
        this.users = data.filter(user => 
          user.role === UserRole.EMPLOYEE || user.role === UserRole.HR
        );
        // Ensure projectTitles is initialized
        this.users.forEach(user => {
          if (!user.projectTitles) {
            user.projectTitles = [];
          }
        });
        console.log('Users loaded successfully:', this.users.length, 'users found');
        this.expandedRows = new Array(this.users.length).fill(false);
        this.errorMessage = null;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = this.getErrorMessage(err.status);
        console.error('Error loading users:', err);
        this.showSnackbar(this.errorMessage);
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  private loadFeaturedProjects(): void {
    this.featuredProjects = [
      { title: 'Cloud Migration Platform', description: 'Enterprise cloud migration...', position: 'DevOps Engineer', subProjects: [{ title: 'AWS Integration', description: 'Migrate servers...' }, { title: 'CI/CD Pipeline', description: 'Automate...' }] },
      { title: 'AI-Powered Analytics Suite', description: 'Machine learning platform...', position: 'Data Scientist', subProjects: [{ title: 'Data Pipeline', description: 'Process raw...' }, { title: 'Prediction Model', description: 'Build forecasting...' }] },
      { title: 'Next-Gen UI Framework', description: 'Modern web development...', position: 'Web Developer', subProjects: [{ title: 'Component Library', description: 'Reusable...' }, { title: 'Responsive Design', description: 'Optimize...' }] },
      { title: 'Quality Assurance Suite', description: 'Automated testing...', position: 'QA Engineer', subProjects: [{ title: 'Unit Tests', description: 'Test individual...' }, { title: 'E2E Testing', description: 'End-to-end...' }] },
      { title: 'Enterprise CRM', description: 'Custom CRM...', position: 'Software Engineer', subProjects: [{ title: 'Customer Database', description: 'Store client...' }, { title: 'Workflow Automation', description: 'Automate...' }] },
      { title: 'Product Roadmap Tool', description: 'Planning and tracking...', position: 'Product Manager', subProjects: [{ title: 'Roadmap UI', description: 'Visualize...' }, { title: 'Task Tracker', description: 'Monitor...' }] },
      { title: 'User Experience Revamp', description: 'Redesigning interfaces...', position: 'UX Designer', subProjects: [{ title: 'Wireframes', description: 'Design initial...' }, { title: 'User Testing', description: 'Validate...' }] },
      { title: 'Server Management Suite', description: 'Tools for system...', position: 'System Administrator', subProjects: [{ title: 'Monitoring Tool', description: 'Track server...' }, { title: 'Backup System', description: 'Automated...' }] },
      { title: 'Tech Strategy Platform', description: 'Leadership tools...', position: 'Technical Lead', subProjects: [{ title: 'Strategy Dashboard', description: 'Visualize...' }, { title: 'Team Sync', description: 'Coordinate...' }] },
      { title: 'Innovation Hub', description: 'Driving company-wide...', position: 'CTO', subProjects: [{ title: 'R&D Portal', description: 'Explore new...' }, { title: 'Innovation Tracker', description: 'Monitor...' }] }
    ];
  }

  private getErrorMessage(status: number): string {
    switch (status) {
      case 404: return 'No users found';
      case 403: return 'You are not authorized to view users';
      case 500: return 'Server error occurred while loading users';
      default: return 'Failed to load users. Please try again.';
    }
  }

  assignProject(userId: string, projectTitle: string): void {
    if (!projectTitle || projectTitle.trim() === '') {
      this.showSnackbar('Project title cannot be empty', 2000);
      return;
    }

    if (this.operationInProgress) {
      this.showSnackbar('Another operation is in progress, please wait', 2000);
      return;
    }

    this.operationInProgress = true;
    this.isAssigningProject[userId] = true;
    console.log(`Starting project assignment - User ID: ${userId}, Project: ${projectTitle}`);

    this.userService.assignProject(userId, projectTitle)
      .pipe(
        finalize(() => {
          this.isAssigningProject[userId] = false;
          this.operationInProgress = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (updatedUser: UserDTO) => {
          console.log(`Project ${projectTitle} assigned successfully`, updatedUser);
          this.showSnackbar(`Assigned ${projectTitle} to user successfully`, 2000);

          // Update local user data
          const userIndex = this.users.findIndex(u => u.userId === userId);
          if (userIndex !== -1) {
            // Ensure updatedUser has all expected properties
            if (updatedUser && updatedUser.projectTitles) {
              // Find the project to get position
              const project = this.featuredProjects.find(p => p.title === projectTitle);
              
              // Update the user in our local array
              this.users[userIndex] = {
                ...this.users[userIndex],
                ...updatedUser,
                projectTitles: [...(updatedUser.projectTitles || [])],
                position: project ? project.position : (updatedUser.position || this.users[userIndex].position || '')
              };
              
              console.log('Local user data updated:', this.users[userIndex]);
            } else {
              console.warn('Received incomplete user data from API', updatedUser);
              // Force reload to get fresh data
              this.loadUserDetails();
            }
          } else {
            console.warn(`User ${userId} not found in local array`);
            // Reload all users to ensure consistency
            this.loadUserDetails();
          }
          
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error assigning project:', err);
          this.showSnackbar(`Failed to assign project: ${err.error || 'Unknown error'}`, 3500);
          // Reload to ensure data consistency
          this.loadUserDetails();
        }
      });
  }

  deassignProject(userId: string, projectTitle: string): void {
    if (confirm(`Are you sure you want to remove ${projectTitle} from this user?`)) {
      if (this.operationInProgress) {
        this.showSnackbar('Another operation is in progress, please wait', 2000);
        return;
      }

      this.operationInProgress = true;
      this.isDeassigningProject[userId] = true;
      console.log(`Starting project deassignment - User ID: ${userId}, Project: ${projectTitle}`);

      this.userService.deassignProject(userId, projectTitle)
        .pipe(
          finalize(() => {
            this.isDeassigningProject[userId] = false;
            this.operationInProgress = false;
            this.cdr.detectChanges();
          })
        )
        .subscribe({
          next: (updatedUser: UserDTO) => {
            console.log(`Project ${projectTitle} deassigned successfully`, updatedUser);
            this.showSnackbar(`Removed ${projectTitle} from user successfully`, 2000);

            // Update local user data
            const userIndex = this.users.findIndex(u => u.userId === userId);
            if (userIndex !== -1) {
              if (updatedUser && Array.isArray(updatedUser.projectTitles)) {
                // Update user with fresh data from the response
                this.users[userIndex] = {
                  ...this.users[userIndex],
                  ...updatedUser,
                  projectTitles: [...(updatedUser.projectTitles || [])]
                };
                
                // Update position if needed
                const currentProjects = this.users[userIndex].projectTitles ?? [];
                this.users[userIndex].position = currentProjects.length > 0 ?
                  (this.users[userIndex].position ?? '') : '';
                
                console.log('Local user data updated after deassignment:', this.users[userIndex]);
              } else {
                console.warn('Received incomplete user data from API during deassignment', updatedUser);
                // Remove the project locally as fallback, safely handling undefined
                const currentProjects = this.users[userIndex].projectTitles ?? [];
                this.users[userIndex].projectTitles = currentProjects.filter(
                  pt => pt !== projectTitle
                );
                this.users[userIndex].position = currentProjects.length > 1 ? 
                  (this.users[userIndex].position ?? '') : '';
                
                // Force reload to get fresh data
                this.loadUserDetails();
              }
            } else {
              console.warn(`User ${userId} not found in local array during deassignment`);
              // Reload all users to ensure consistency
              this.loadUserDetails();
            }
            
            this.cdr.detectChanges();
          },
          error: (err) => {
            console.error('Error deassigning project:', err);
            let errorMessage = err.error || 'Unknown error';
            
            if (err.status === 404) {
              errorMessage = 'User not found';
            } else if (err.status === 400 && typeof err.error === 'string' && err.error.includes('Project not assigned')) {
              // Project wasn't actually assigned - remove it from our local state
              const userIndex = this.users.findIndex(u => u.userId === userId);
              if (userIndex !== -1) {
                const currentProjects = this.users[userIndex].projectTitles ?? [];
                this.users[userIndex].projectTitles = currentProjects.filter(
                  pt => pt !== projectTitle
                );
                this.users[userIndex].position = currentProjects.length > 1 ?
                  (this.users[userIndex].position ?? '') : '';
              }
              errorMessage = `Project was not assigned to this user`;
            }
            
            this.showSnackbar(`Operation result: ${errorMessage}`, 3500);
            // Reload to ensure data consistency
            this.loadUserDetails();
          }
        });
    }
  }

  private showSnackbar(message: string, duration: number = 3000): void {
    this.snackBar.open(message, 'Close', { duration });
  }

  toggleDetails(index: number): void {
    this.expandedRows[index] = !this.expandedRows[index];
  }

  isProjectBeingAssigned(userId: string): boolean {
    return this.isAssigningProject[userId] ?? false;
  }
  
  isProjectBeingDeassigned(userId: string): boolean {
    return this.isDeassigningProject[userId] ?? false;
  }

  isOperationInProgress(userId: string): boolean {
    return this.isProjectBeingAssigned(userId) || this.isProjectBeingDeassigned(userId);
  }

  getAssignedProjectTitle(user: UserDTO): string {
    if (user.projectTitles && user.projectTitles.length > 0) {
      return user.projectTitles.join(', ');
    }
    return 'None';
  }

  getAssignedProjects(user: UserDTO): string {
    if (user.projectTitles && user.projectTitles.length > 0) {
      return user.projectTitles.join(', ');
    }
    return 'None';
  }
}