import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserDTO, UserRole, UserStatus } from 'src/app/Models/user.models';
import { KeycloakService } from 'src/app/Service/KeycloakService';
import { UserService } from 'src/app/Service/UserService';

interface CompanyProject {
  title: string;
  description: string;
  position: string;
  category: string;
  technologies: string[];
  progress: number;
  deadline?: string;
}

interface ProjectStats {
  totalProjects: number;
  totalUsers: number;
  categoryCounts: { [key: string]: number };
  averageTeamSize: number;
}

@Component({
  selector: 'app-acceuil',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './acceuil.component.html',
  styleUrls: ['./acceuil.component.css']
})
export class AcceuilComponent implements OnInit {
  featuredProjects: CompanyProject[] = [];
  filteredProjects: CompanyProject[] = [];
  users: UserDTO[] = [];
  selectedProject: CompanyProject | null = null;
  isLoaded = false;
  categories: string[] = [];
  selectedCategory: string = 'All';
  searchTerm: string = '';
  currentUser: UserDTO | null = null;

  projectStats: ProjectStats = {
    totalProjects: 0,
    totalUsers: 0,
    categoryCounts: {},
    averageTeamSize: 0
  };

  constructor(
    private keycloakService: KeycloakService,
    private userService: UserService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadCurrentUser();
    this.loadFeaturedProjects();
    this.loadUsers();
    setTimeout(() => {
      this.isLoaded = true;
      this.cdr.detectChanges();
    }, 300);
  }

  private loadCurrentUser(): void {
    const userId = this.keycloakService.getUserId();
    const username = this.keycloakService.getUsername();
    const role = this.convertToUserRole(this.keycloakService.getRole());
    if (userId && username) {
      this.currentUser = {
        userId,
        username,
        email: this.keycloakService.getEmail() || '',
        role,
        status: 'ACTIVE' as UserStatus,
        projectTitles: []
      };
      // Load current user's assignments from local storage
      const savedData = localStorage.getItem(`user_${userId}_projects`);
      if (savedData) {
        try {
          const { projectTitles, position } = JSON.parse(savedData);
          this.currentUser.projectTitles = Array.isArray(projectTitles) ? projectTitles : [];
          this.currentUser.position = typeof position === 'string' ? position : '';
        } catch (e) {
          console.error(`Error parsing local storage for user ${userId}:`, e);
          this.currentUser.projectTitles = [];
          this.currentUser.position = '';
        }
      }
    }
  }

  private convertToUserRole(roleString: string): UserRole {
    if (!roleString) return UserRole.EMPLOYEE;
    const upperRole = roleString.toUpperCase().replace('ROLE_', '');
    return Object.values(UserRole).includes(upperRole as UserRole)
      ? upperRole as UserRole
      : UserRole.EMPLOYEE;
  }

  private loadFeaturedProjects(): void {
    const allProjects: CompanyProject[] = [
      { 
        title: 'Cloud Migration Platform', 
        description: 'Enterprise cloud migration platform designed to seamlessly transfer on-premise infrastructure to cloud providers. Features automated discovery, dependency mapping, and migration planning tools.', 
        position: 'DevOps Engineer',
        category: 'Infrastructure',
        technologies: ['AWS', 'Azure', 'Terraform', 'Docker', 'Kubernetes'],
        progress: 65
      },
      { 
        title: 'AI-Powered Analytics Suite', 
        description: 'Machine learning platform that transforms raw data into actionable intelligence. Includes data preprocessing, model training, evaluation tools and interactive visualization dashboards.', 
        position: 'Data Scientist',
        category: 'Data & AI',
        technologies: ['Python', 'TensorFlow', 'PyTorch', 'Pandas', 'Scikit-learn'],
        progress: 80,
        deadline: '2025-05-15'
      },
      { 
        title: 'Next-Gen UI Framework', 
        description: 'Modern web development framework focused on component reusability, performance optimization, and design system implementation. Provides a comprehensive library of UI elements.', 
        position: 'Web Developer',
        category: 'Frontend',
        technologies: ['Angular', 'TypeScript', 'SCSS', 'RxJS', 'WebAssembly'],
        progress: 90,
        deadline: '2025-04-30'
      },
      { 
        title: 'Quality Assurance Suite', 
        description: 'Automated testing framework for ensuring software reliability. Features end-to-end testing, integration testing, performance benchmarking, and continuous testing pipelines.', 
        position: 'QA Engineer',
        category: 'Quality Assurance',
        technologies: ['Selenium', 'Jest', 'Cypress', 'JUnit', 'JMeter'],
        progress: 75
      },
      { 
        title: 'Enterprise CRM', 
        description: 'Custom CRM solution designed for large enterprise needs. Includes contact management, sales pipeline tracking, reporting, integration with marketing automation, and AI-driven insights.', 
        position: 'Software Engineer',
        category: 'Business Applications',
        technologies: ['Java', 'Spring Boot', 'PostgreSQL', 'React', 'Redis'],
        progress: 60,
        deadline: '2025-06-30'
      },
      { 
        title: 'Product Roadmap Tool', 
        description: 'Planning and tracking tool for product development teams. Features backlog management, sprint planning, resource allocation, and release management capabilities.', 
        position: 'Product Manager',
        category: 'Business Applications',
        technologies: ['Node.js', 'Express', 'MongoDB', 'Vue.js', 'D3.js'],
        progress: 40
      },
      { 
        title: 'User Experience Revamp', 
        description: 'Redesigning interfaces across all company products to improve usability, accessibility, and visual consistency. Includes user research, prototyping, and design system creation.', 
        position: 'UX Designer',
        category: 'Frontend',
        technologies: ['Figma', 'Sketch', 'Adobe XD', 'InVision', 'Zeplin'],
        progress: 85,
        deadline: '2025-05-10'
      },
      { 
        title: 'Server Management Suite', 
        description: 'Tools for system administrators to monitor, manage, and optimize server infrastructure. Features performance monitoring, automated patching, and security vulnerability scanning.', 
        position: 'System Administrator',
        category: 'Infrastructure',
        technologies: ['Linux', 'Ansible', 'Nagios', 'Prometheus', 'Grafana'],
        progress: 70
      },
      { 
        title: 'Tech Strategy Platform', 
        description: 'Leadership tools for technology planning, architecture governance, and investment tracking. Supports IT portfolio management and strategic roadmapping.', 
        position: 'Technical Lead',
        category: 'Leadership & Strategy',
        technologies: ['Java', 'Spring', 'Angular', 'PostgreSQL', 'Elasticsearch'],
        progress: 55
      },
      { 
        title: 'Innovation Hub', 
        description: 'Driving company-wide innovation through ideation tools, prototype development resources, and innovation metrics tracking. Supports the entire innovation lifecycle.', 
        position: 'CTO',
        category: 'Leadership & Strategy',
        technologies: ['Node.js', 'GraphQL', 'React', 'MongoDB', 'WebSockets'],
        progress: 30,
        deadline: '2025-08-15'
      }
    ];

    // Filter projects for EMPLOYEE users
    if (this.currentUser?.role === UserRole.EMPLOYEE) {
      this.featuredProjects = allProjects.filter(project =>
        this.currentUser?.projectTitles?.includes(project.title) ?? false
      );
    } else {
      this.featuredProjects = allProjects;
    }

    // Extract unique categories from filtered projects
    this.categories = ['All', ...new Set(this.featuredProjects.map(p => p.category))];
    this.filteredProjects = [...this.featuredProjects];
    this.calculateStats();
  }

  private loadUsers(): void {
    this.userService.getAllUsers().subscribe({
      next: (users) => {
        this.users = users;
        // Load project assignments from local storage
        this.users.forEach(user => {
          if (!user.projectTitles) {
            user.projectTitles = [];
          }
          const savedData = localStorage.getItem(`user_${user.userId}_projects`);
          if (savedData) {
            try {
              const { projectTitles, position } = JSON.parse(savedData);
              user.projectTitles = Array.isArray(projectTitles) ? projectTitles : [];
              user.position = typeof position === 'string' ? position : user.position || '';
            } catch (e) {
              console.error(`Error parsing local storage for user ${user.userId}:`, e);
              user.projectTitles = [];
              user.position = '';
            }
          }
        });
        this.calculateStats();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading users:', err);
        this.users = [];
        this.calculateStats();
        this.cdr.detectChanges();
      }
    });
  }

  private calculateStats(): void {
    this.projectStats.totalProjects = this.filteredProjects.length;
    this.projectStats.totalUsers = this.users.length;
    this.projectStats.categoryCounts = {};

    // Calculate category counts based on filtered projects
    this.categories.forEach(category => {
      if (category !== 'All') {
        this.projectStats.categoryCounts[category] = this.filteredProjects.filter(
          p => p.category === category
        ).length;
      }
    });

    // Calculate average team size
    let totalAssignedUsers = 0;
    this.filteredProjects.forEach(project => {
      totalAssignedUsers += this.getUserCountForProject(project.title);
    });
    this.projectStats.averageTeamSize = this.projectStats.totalProjects
      ? totalAssignedUsers / this.projectStats.totalProjects
      : 0;
  }

  filterByCategory(category: string): void {
    this.selectedCategory = category;
    this.applyFilters();
  }

  searchProjects(term: string): void {
    this.searchTerm = term;
    this.applyFilters();
  }

  private applyFilters(): void {
    this.filteredProjects = this.featuredProjects.filter(project => {
      // Apply category filter
      const categoryMatch = this.selectedCategory === 'All' || project.category === this.selectedCategory;
      // Apply search filter
      const searchMatch = !this.searchTerm ||
        project.title.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        project.description.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        project.position.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        project.technologies.some(tech => tech.toLowerCase().includes(this.searchTerm.toLowerCase()));
      return categoryMatch && searchMatch;
    });
    this.calculateStats();
    this.cdr.detectChanges();
  }

  getUsersForProject(projectTitle: string): UserDTO[] {
    return this.users.filter(user => user.projectTitles?.includes(projectTitle) ?? false);
  }

  getImageForProject(projectTitle: string): string {
    const project = this.featuredProjects.find(p => p.title === projectTitle);
    const imageMap: { [key: string]: string } = {
      'DevOps Engineer': 'assets/devops-engineer.jpg',
      'Data Scientist': 'assets/data-scientist.jpg',
      'Web Developer': 'assets/web-developer.jpg',
      'QA Engineer': 'assets/qa-engineer.jpg',
      'Software Engineer': 'assets/software-engineer.jpg',
      'Product Manager': 'assets/product-manager.jpg',
      'UX Designer': 'assets/ux-designer.jpg',
      'System Administrator': 'assets/system-admin.jpg',
      'Technical Lead': 'assets/technical-lead.jpg',
      'CTO': 'assets/cto.jpg'
    };
    return project ? (imageMap[project.position] || 'assets/default-project.jpg') : 'assets/default-project.jpg';
  }

  selectProject(project: CompanyProject): void {
    this.selectedProject = project;
    this.cdr.detectChanges();
  }

  deselectProject(): void {
    this.selectedProject = null;
    this.cdr.detectChanges();
  }

  getUserCountForProject(projectTitle: string): number {
    return this.getUsersForProject(projectTitle).length;
  }

  getProgressColorClass(progress: number): string {
    if (progress < 40) return 'progress-low';
    if (progress < 70) return 'progress-medium';
    return 'progress-high';
  }

  
}