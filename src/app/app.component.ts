import { Component, OnInit } from '@angular/core';
import { KeycloakService } from './Service/KeycloakService';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  template: '<router-outlet></router-outlet>'
})
export class AppComponent implements OnInit {
  private isVideoCallInitializing = false;

  constructor(
    private keycloakService: KeycloakService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.isVideoCallInitializing = this.router.url.startsWith('/video-call');
    console.log('AppComponent: Initial URL:', this.router.url, 'isVideoCallInitializing:', this.isVideoCallInitializing);

    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd)
    ).subscribe((event: NavigationEnd) => {
      console.log('AppComponent: Navigated to', event.url);
      this.isVideoCallInitializing = event.url.startsWith('/video-call');
    });

    this.initializeAuth();
  }

  private initializeAuth(): void {
    this.keycloakService.isInitialized().subscribe(initialized => {
      if (initialized) {
        this.keycloakService.getAuthObservable().subscribe({
          next: (authenticated) => {
            if (authenticated !== null) {
              this.handleAuthState(authenticated);
            }
          },
          error: (err) => this.handleError(err)
        });
      }
    });
  }

  private handleAuthState(authenticated: boolean): void {
  const currentUrl = this.router.url;
  console.log('Auth state changed:', { authenticated, currentUrl });

  // Skip redirect logic if we're on video-call route
  if (currentUrl.startsWith('/video-call')) {
    console.log('On video-call route, skipping redirect');
    return;
  }

  if (authenticated) {
    if (!currentUrl.startsWith('/acceuil')) {
      this.router.navigate(['/acceuil']);
    }
  } else {
    // Handle unauthenticated state for non-video-call routes
    if (!currentUrl.startsWith('/video-call')) {
      this.keycloakService.login();
    }
  }
}

  private handleError(error: any): void {
    console.error('AppComponent: Authentication error:', error);
    console.log('AppComponent: KeycloakService will handle error redirect');
  }
}