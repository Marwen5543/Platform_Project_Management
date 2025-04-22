import { Component, OnInit } from '@angular/core';
import { KeycloakService } from './Service/KeycloakService';
import { Router } from '@angular/router';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  template: '<router-outlet></router-outlet>'
})
export class AppComponent implements OnInit {
  constructor(
    private keycloakService: KeycloakService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initializeAuth();
  }

  private initializeAuth(): void {
    this.keycloakService.getAuthObservable().pipe(
      filter(authenticated => authenticated !== null)
    ).subscribe({
      next: (authenticated) => this.handleAuthState(authenticated),
      error: (err) => this.handleError(err)
    });
  }

  private handleAuthState(authenticated: boolean): void {
    if (authenticated) {
      if (this.router.url !== '/acceuil') { 
        this.router.navigate(['/acceuil']);
      }
    } else {
      this.router.navigate(['/login']);
    }
  }

  private handleError(error: any): void {
    console.error('Authentication error:', error);
    this.router.navigate(['/login']);
  }
}
