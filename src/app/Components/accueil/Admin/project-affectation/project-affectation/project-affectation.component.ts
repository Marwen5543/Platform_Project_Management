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
      projectTitles: [] // Initialize as empty array
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
        // Initialize projectTitles and load from local storage
        this.users.forEach(user => {
          if (!user.projectTitles) {
            user.projectTitles = [];
          }
          const savedData = localStorage.getItem(`user_${user.userId}_projects`);
          if (savedData) {
            const { projectTitles, position } = JSON.parse(savedData);
            user.projectTitles = Array.isArray(projectTitles) ? projectTitles : [];
            user.position = typeof position === 'string' ? position : user.position || '';
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
    if (!projectTitle) return;

    this.isAssigningProject[userId] = true;
    console.log(`Starting project assignment - User ID: ${userId}, Project: ${projectTitle}`);
    this.cdr.detectChanges();

    const user = this.users.find(u => u.userId === userId);
    if (!user) {
      console.warn(`User not found: ${userId}`);
      this.showSnackbar(`User not found`, 2000);
      this.isAssigningProject[userId] = false;
      this.cdr.detectChanges();
      return;
    }

    // Initialize projectTitles if undefined
    if (!user.projectTitles) {
      user.projectTitles = [];
    }

    if (!user.projectTitles.includes(projectTitle)) {
      user.projectTitles.push(projectTitle);
      const project = this.featuredProjects.find(p => p.title === projectTitle);
      if (project) {
        user.position = project.position;
      }

      // Save to local storage
      localStorage.setItem(
        `user_${userId}_projects`,
        JSON.stringify({ projectTitles: user.projectTitles, position: user.position || '' })
      );

      // Simulate backend assignment
      this.userService.assignProjects(user).subscribe({
        next: (updatedUser) => {
          console.log(`Project assigned locally:`, updatedUser);
          this.showSnackbar(`Assigned ${user.username} to ${projectTitle}`, 2000);
        },
        error: (err) => {
          console.error(`Error simulating assignment:`, err);
          this.showSnackbar(`Failed to assign ${user.username} to ${projectTitle}`, 2000);
          // Revert changes
          user.projectTitles = user.projectTitles!.filter(pt => pt !== projectTitle);
          user.position = ''; // Reset position if needed
          localStorage.setItem(
            `user_${userId}_projects`,
            JSON.stringify({ projectTitles: user.projectTitles, position: user.position })
          );
        },
        complete: () => {
          this.isAssigningProject[userId] = false;
          this.cdr.detectChanges();
        }
      });
    } else {
      this.showSnackbar(`${user.username} is already assigned to ${projectTitle}`, 2000);
      this.isAssigningProject[userId] = false;
      this.cdr.detectChanges();
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

  getAssignedProjectTitle(user: UserDTO): string {
    // Safely handle projectTitles
    if (user.projectTitles && user.projectTitles.length > 0) {
      return user.projectTitles.join(', ');
    }
    return this.featuredProjects.find(p => p.position === user.position)?.title || 'Not Assigned';
  }

  getAssignedProjects(user: UserDTO): string {
    // Safely handle projectTitles
    if (user.projectTitles && user.projectTitles.length > 0) {
      return user.projectTitles.join(', ');
    }
    return 'None';
  }
}