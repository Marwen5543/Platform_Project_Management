// app.component.ts
import { Component, OnInit } from '@angular/core';
import { KeycloakService } from './Service/KeycloakService';
import { Router } from '@angular/router';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  template: '<router-outlet></router-outlet>'

})
export class AppComponent implements OnInit {
  showHeader = false;
  showSidebar = false;

  constructor(
    private keycloakService: KeycloakService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initializeAuth();
  }

  private initializeAuth(): void {
    this.keycloakService.isInitialized().pipe(
      filter(initialized => initialized)
    ).subscribe({
      next: () => this.handleAuthState(),
      error: (err) => this.handleInitializationError(err)
    });
  }

  private handleAuthState(): void {
    this.keycloakService.getAuthObservable().subscribe({
      next: (authenticated) => {
        this.showHeader = authenticated;
        this.showSidebar = authenticated;
        authenticated ? this.redirectToSecuredArea() : this.redirectToPublicArea();
      },
      error: (err) => this.handleAuthError(err)
    });
  }

  private redirectToSecuredArea(): void {
    this.router.navigate(['/acceuil']).catch(() => {
      window.location.href = '/acceuil';
    });
  }

  private redirectToPublicArea(): void {
    this.router.navigate(['/login']).catch(() => {
      window.location.href = '/login';
    });
  }

  private handleInitializationError(error: any): void {
    console.error('Initialization error:', error);
    this.redirectToPublicArea();
  }

  private handleAuthError(error: any): void {
    console.error('Authentication error:', error);
    this.redirectToPublicArea();
  }
}
