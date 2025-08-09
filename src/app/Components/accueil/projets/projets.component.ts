import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { UserDTO, UserRole } from 'src/app/Models/user.models';
import { KeycloakService } from 'src/app/Service/KeycloakService';
import { UserService } from 'src/app/Service/UserService';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { forkJoin } from 'rxjs';


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
  selector: 'app-projets',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './projets.component.html',
  styleUrl: './projets.component.css'
})
export class ProjetsComponent implements OnInit, OnDestroy{
 featuredProjects: CompanyProject[] = [];
  filteredProjects: CompanyProject[] = [];
  users: UserDTO[] = [];
  selectedProject: CompanyProject | null = null;
  isLoaded = false;
  categories: string[] = ['All'];
  selectedCategory: string = 'All';
  searchTerm: string = '';
  currentUser: UserDTO | null = null;
  errorMessage: string | null = null;
  isLoading: boolean = true; 
  projectStats: ProjectStats = {
    totalProjects: 0,
    totalUsers: 0,
    categoryCounts: {},
    averageTeamSize: 0
  };

  bg1Url!: string;
  bg2Url!: string;
  isBg1Visible = true;
  private currentImageIndex = 0;
  private intervalId: any;
  private allBackgroundImages = [
    '/assets/carousel-1.jpg', // IMPORTANT: Make sure this path is correct!
  ];

  constructor(
    private keycloakService: KeycloakService,
    private userService: UserService,
    private cdr: ChangeDetectorRef,
    private router: Router,
  ) {}
  ngOnDestroy(): void {
     if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }

  ngOnInit(): void {
  this.isLoading = true;
  forkJoin({
    currentUser: this.keycloakService.getCurrentUser(),
    users: this.userService.getAllUsers()
  }).pipe(
    finalize(() => {
      this.isLoading = false;
      this.cdr.detectChanges();
    })
  ).subscribe({
    next: ({ currentUser, users }) => {
      if (currentUser && currentUser.userId && currentUser.username) {
        this.userService.getUserDetailsById(currentUser.userId).subscribe({
          next: (userDetails: UserDTO) => {
            this.currentUser = {
              ...currentUser,
              projectTitles: userDetails.projectTitles ?? [],
              position: userDetails.position ?? ''
            };
            console.log('Loaded current user', this.currentUser);
            this.loadFeaturedProjects();
            this.cdr.detectChanges();
          },
          error: (err) => {
            console.error('Error fetching user details:', err);
            this.currentUser = { ...currentUser, projectTitles: [], position: '' };
            this.errorMessage = 'Failed to load user projects.';
            this.loadFeaturedProjects();
            this.cdr.detectChanges();
          }
        });
      } else {
        this.currentUser = null;
        this.errorMessage = 'User authentication failed.';
      }
      this.users = users;
      this.calculateStats();
      this.cdr.detectChanges();
    },
    error: (err) => {
      console.error('Error loading data:', err);
      this.errorMessage = 'Failed to load data.';
      this.currentUser = null;
      this.users = [];
      this.cdr.detectChanges();
    }
  });
   this.bg1Url = this.allBackgroundImages[0];
    this.bg2Url = this.allBackgroundImages[1];
    this.startImageRotation();

    // 2. Trigger the text fade-in animation after a short delay
    setTimeout(() => {
      this.isLoaded = true;
      this.cdr.detectChanges(); // This tells Angular to apply the 'loaded' class
    }, 100);
}
  startImageRotation() {
    this.intervalId = setInterval(() => {
      this.currentImageIndex = (this.currentImageIndex + 1) % this.allBackgroundImages.length;
      const nextImage = this.allBackgroundImages[this.currentImageIndex];

      if (this.isBg1Visible) {
        this.bg2Url = nextImage;
      } else {
        this.bg1Url = nextImage;
      }
      this.isBg1Visible = !this.isBg1Visible;
    }, 7000); // Change image every 7 seconds
  }


  getBackgroundImageUrl(imageUrl: string): string {
    return `linear-gradient(135deg, rgba(180, 20, 20, 0.75), rgba(80, 0, 0, 0.85)), url('${imageUrl}')`;
  }



  public loadCurrentUser(): void {
    this.keycloakService.getCurrentUser().subscribe({
      next: (user: UserDTO) => {
        if (user && user.userId && user.username) {
          this.currentUser = user;
          // Fetch the current user's details from the backend to get projectTitles
          this.userService.getUserDetailsById(user.userId).subscribe({
            next: (userDetails: UserDTO) => {
              this.currentUser = {
                ...user,
                projectTitles: userDetails.projectTitles ?? [],
                position: userDetails.position ?? ''
              };
              console.log('Loaded current user', this.currentUser);
              // Reload featured projects since it depends on currentUser.projectTitles
              this.loadFeaturedProjects();
              this.cdr.detectChanges();
            },
            error: (err: any) => {
              console.error('Error fetching current user details:', err);
              this.currentUser = {
                ...user,
                projectTitles: [],
                position: ''
              };
              this.errorMessage = 'Failed to load user projects. Showing default view.';
              this.loadFeaturedProjects();
              this.cdr.detectChanges();
            }
          });
        } else {
          console.error('Failed to load user: missing userId or username', user);
          this.errorMessage = 'User authentication failed. Please log in again.';
          this.currentUser = null;
          this.cdr.detectChanges();
        }
      },
      error: (err: any) => {
        console.error('Error getting current user from Keycloak:', err);
        this.errorMessage = 'User authentication failed. Please log in again.';
        this.currentUser = null;
        this.cdr.detectChanges();
      }
    });
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
        description: "Plateforme de migration cloud pour les entreprises, conçue pour transférer de manière fluide l'infrastructure sur site vers des fournisseurs cloud. Inclut des outils d-discovery automatisée, de cartographie des dépendances et de planification de migration.",
        position: 'DevOps Engineer',
        category: 'Infrastructure',
        technologies: ['AWS', 'Azure', 'Terraform', 'Docker', 'Kubernetes'],
        progress: 65
      },
      {
        title: 'AI-Powered Analytics Suite',
        description: "Une plateforme d'apprentissage automatique qui transforme les données brutes en insights exploitables. Elle offre des outils pour le prétraitement des données et l'entraînement de modèles d'IA. Des outils d'évaluation permettent de mesurer la performance des modèles. Des tableaux de bord interactifs facilitent la visualisation des résultats et des performances. Cette plateforme simplifie le processus de création et d'optimisation de modèles d'IA pour des applications pratiques.",
        position: 'Data Scientist',
        category: 'Data & AI',
        technologies: ['Python', 'TensorFlow', 'PyTorch', 'Pandas', 'Scikit-learn'],
        progress: 80,
        deadline: '2025-05-15'
      },
      {
        title: 'Next-Gen UI Framework',
        description: "Un framework de développement web moderne, conçu pour favoriser la réutilisabilité des composants et l'optimisation des performances. Il permet l'implémentation de systèmes de design cohérents et efficaces. Il propose une bibliothèque complète d'éléments d'interface utilisateur, simplifiant ainsi le développement d'applications interactives. Il offre des outils puissants pour la gestion de l'état, la navigation et les animations. Ce framework est idéal pour construire des applications web évolutives et maintenables.",
        position: 'Web Developer',
        category: 'Frontend',
        technologies: ['Angular', 'TypeScript', 'SCSS', 'RxJS', 'WebAssembly'],
        progress: 90,
        deadline: '2025-04-30'
      },
      {
        title: 'Quality Assurance Suite',
        description: 'Un framework de tests automatisés pour garantir la fiabilité des logiciels. Il inclut des tests de bout en bout, des tests d"intégration et des benchmarks de performance. Des pipelines de tests continus assurent une validation constante du code. Il permet d"automatiser les processus de test pour détecter rapidement les problèmes. Ce framework améliore la qualité du logiciel tout en réduisant les erreurs humaines.',
        position: 'QA Engineer',
        category: 'Quality Assurance',
        technologies: ['Selenium', 'Jest', 'Cypress', 'JUnit', 'JMeter'],
        progress: 75
      },
      {
        title: 'Enterprise CRM',
        description: "Solution CRM personnalisée conçue pour les besoins des grandes entreprises. Elle inclut la gestion des contacts, le suivi des pipelines de vente et des rapports détaillés. L'intégration avec l'automatisation du marketing permet une gestion fluide des campagnes. Des insights basés sur l'IA offrent des recommandations pour améliorer les performances commerciales. Cette solution centralise les données pour une prise de décision plus éclairée et stratégique.",
        position: 'Software Engineer',
        category: 'Business Applications',
        technologies: ['Java', 'Spring Boot', 'PostgreSQL', 'React', 'Redis'],
        progress: 60,
        deadline: '2025-06-30'
      },
      {
        title: 'Product Roadmap Tool',
        description: "Outil de planification et de suivi pour les équipes de développement de produits. Il comprend la gestion du backlog, la planification des sprints, l'allocation des ressources et des fonctionnalités de gestion des versions. Ce système facilite la coordination entre les membres de l'équipe et assure le suivi des progrès tout au long du cycle de développement. Il aide à prioriser les tâches et à livrer les produits dans les délais. L'outil optimise la gestion des projets en offrant une vue d'ensemble claire et en temps réel.",
        position: 'Product Manager',
        category: 'Business Applications',
        technologies: ['Node.js', 'Express', 'MongoDB', 'Vue.js', 'D3.js'],
        progress: 40
      },
      {
        title: 'User Experience Revamp',
        description: "Refonte des interfaces de tous les produits de l'entreprise pour améliorer l'ergonomie, l'accessibilité et la cohérence visuelle. Ce projet inclut la recherche utilisateur, la création de prototypes et le développement d'un système de design. L'objectif est d'uniformiser l'expérience utilisateur tout en répondant aux besoins spécifiques des utilisateurs. Le processus implique une évaluation approfondie des interfaces actuelles et l'intégration de solutions basées sur les meilleures pratiques en matière de design. Cela permet d'offrir une expérience plus fluide et intuitive pour les utilisateurs.",
        position: 'UX Designer',
        category: 'Frontend',
        technologies: ['Figma', 'Sketch', 'Adobe XD', 'InVision', 'Zeplin'],
        progress: 85,
        deadline: '2025-05-10'
      },
      {
        title: 'Server Management Suite',
        description: "Outils pour les administrateurs système permettant de surveiller, gérer et optimiser l'infrastructure des serveurs. Ils incluent la surveillance des performances, la mise à jour automatique, ainsi que l'analyse des vulnérabilités de sécurité. Ces outils aident à garantir la disponibilité et la sécurité des serveurs tout en réduisant les risques de pannes. Ils offrent une gestion centralisée, permettant aux administrateurs de réagir rapidement aux problèmes et d'améliorer l'efficacité opérationnelle. Grâce à des rapports détaillés, ces outils facilitent la prise de décision pour maintenir une infrastructure optimale.",
        position: 'System Administrator',
        category: 'Infrastructure',
        technologies: ['Linux', 'Ansible', 'Nagios', 'Prometheus', 'Grafana'],
        progress: 70
      },
      {
        title: 'Tech Strategy Platform',
        description: "Outils de leadership pour la planification technologique, la gouvernance de l'architecture et le suivi des investissements. Ils soutiennent la gestion du portefeuille informatique et la création de feuilles de route stratégiques. Ces outils permettent aux responsables technologiques de prendre des décisions éclairées, d'aligner les initiatives sur les objectifs d'affaires et de suivre l'évolution des projets. Ils offrent une vue d'ensemble de l'architecture IT, facilitent la gestion des ressources et assurent une meilleure allocation des investissements. Grâce à des outils de suivi, les entreprises peuvent anticiper les besoins futurs et optimiser leur infrastructure technologique.",
        position: 'Technical Lead',
        category: 'Leadership & Strategy',
        technologies: ['Java', 'Spring', 'Angular', 'PostgreSQL', 'Elasticsearch'],
        progress: 55
      },
      {
        title: 'Innovation Hub',
        description: "Outils permettant de stimuler l'innovation à l'échelle de l'entreprise grâce à des ressources de génération d'idées, de développement de prototypes et de suivi des indicateurs d'innovation. Ces outils soutiennent l'ensemble du cycle de vie de l'innovation, de la conception initiale à la mise en œuvre. Ils facilitent la collaboration entre les équipes, permettent de transformer les idées en prototypes fonctionnels et mesurent l'impact des initiatives innovantes. En fournissant une plateforme pour évaluer les projets d'innovation, ces outils aident les entreprises à rester compétitives et à anticiper les tendances du marché.",
        position: 'CTO',
        category: 'Leadership & Strategy',
        technologies: ['Node.js', 'GraphQL', 'React', 'MongoDB', 'WebSockets'],
        progress: 30,
        deadline: '2025-08-15'
      }
    ];

    if (this.currentUser?.role === UserRole.EMPLOYEE) {
      const projectTitles = this.currentUser?.projectTitles ?? [];
      this.featuredProjects = allProjects.filter(project =>
        projectTitles.includes(project.title)
      );
      console.log(`Filtered projects for EMPLOYEE ${this.currentUser.username} (${this.currentUser.userId}):`, this.featuredProjects.map(p => p.title));
    } else {
      this.featuredProjects = allProjects;
      console.log(`Loaded all projects for non-EMPLOYEE ${this.currentUser?.username} (${this.currentUser?.userId}):`, this.featuredProjects.length);
    }

    this.categories = ['All', ...new Set(this.featuredProjects.map(p => p.category))];
    this.filteredProjects = [...this.featuredProjects];
    this.calculateStats();
  }

  private loadUsers(): void {
    this.userService.getAllUsers().subscribe({
      next: (users) => {
        this.users = users;
        this.users.forEach(user => {
          // Ensure projectTitles is initialized (already fetched from backend)
          if (!user.projectTitles) {
            user.projectTitles = [];
          }
          if (!user.position) {
            user.position = '';
          }
        });
        console.log('Users with assignments:', this.users.map(u => ({ userId: u.userId, username: u.username, projectTitles: u.projectTitles })));
        this.calculateStats();
        this.cdr.detectChanges();
      },
      error: (err: any) => {
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

    this.categories.forEach(category => {
      if (category !== 'All') {
        this.projectStats.categoryCounts[category] = this.filteredProjects.filter(
          p => p.category === category
        ).length;
      }
    });

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

  public applyFilters(): void {
    this.filteredProjects = this.featuredProjects.filter(project => {
      const categoryMatch = this.selectedCategory === 'All' || project.category === this.selectedCategory;
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

  startMeeting(): void {
    if (this.selectedProject) {
      this.router.navigate(['/video-call', encodeURIComponent(this.selectedProject.title)]);
    } else {
      console.error('No project selected for meeting');
      alert('Please select a project to start a meeting.');
    }
  }
}

