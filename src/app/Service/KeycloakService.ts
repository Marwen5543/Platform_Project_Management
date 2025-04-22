import { Injectable } from '@angular/core';
import Keycloak, { KeycloakProfile } from 'keycloak-js';
import { keycloakConfig } from '../Config/keycloak-config';
import { BehaviorSubject, Observable, from } from 'rxjs';
import { jwtDecode } from 'jwt-decode';
import { Router, UrlTree } from '@angular/router';

interface DecodedToken {
  preferred_username?: string;
  sub?: string;
  email?: string;
  realm_access?: {
    roles: string[];
  };
}

@Injectable({ providedIn: 'root' })
export class KeycloakService {
  private keycloak: Keycloak.KeycloakInstance;
  private authSubject = new BehaviorSubject<boolean>(false);
  private initializedSubject = new BehaviorSubject<boolean>(false);
  private userProfile = new BehaviorSubject<DecodedToken>({});
  private refreshInterval?: number;

  constructor(private router: Router) {
    console.log('Keycloak config:', keycloakConfig); // Log config at construction
    this.keycloak = new Keycloak(keycloakConfig);
    this.initializeAuth();
  }


  
  getUsername(): string {
    return this.userProfile.value.preferred_username || 'Utilisateur';
  }

  getUserId(): string {
    return this.userProfile.value.sub || '';
  }

  getEmail(): string {
    return this.userProfile.value.email || '';
  }

  // In keycloak.service.ts
  getRole(): string {
    if (!this.keycloak.authenticated) {
      console.warn('Keycloak not authenticated');
      return '';
    }
    
    const roles = this.keycloak.realmAccess?.roles || [];
    console.log('All roles from token:', roles);
    
    // Check for SUPER_ADMIN first
    if (roles.includes('SUPER_ADMIN')) {
      console.log('Found SUPER_ADMIN role');
      return 'SUPER_ADMIN';
    }
    
    // Rest of role hierarchy
    if (roles.includes('ADMIN')) return 'ADMIN';
    if (roles.includes('MANAGER')) return 'MANAGER';
    if (roles.includes('HR')) return 'HR';
    
    return 'EMPLOYEE';
  }

getRoles(): string[] {
  return this.keycloak.realmAccess?.roles || [];
}

  isAuthenticated(): boolean {
    return this.keycloak.authenticated || false;
  }

  getToken(): Promise<string> {
    return this.keycloak.updateToken(5).then(() => this.keycloak.token || '');
  }

  isInitialized(): Observable<boolean> {
    return this.initializedSubject.asObservable();
  }

  getAuthObservable(): Observable<boolean> {
    return this.authSubject.asObservable();
  }

  async canActivate(): Promise<boolean | UrlTree> {
    if (!this.initializedSubject.value) {
      await this.initializedSubject.toPromise();
    }
    return this.isAuthenticated() || this.router.parseUrl('/login');
  }

  login(): Promise<void> {
    return this.keycloak.login();
  }

  logout(): Promise<void> {
    if (this.refreshInterval) {
      window.clearInterval(this.refreshInterval);
    }
    return this.keycloak.logout({ redirectUri: window.location.origin });
  }

  private async initializeAuth(): Promise<void> {
    try {
      console.log('Attempting Keycloak initialization with config:', keycloakConfig);
      const authenticated = await this.keycloak.init({
        onLoad: 'login-required',
        checkLoginIframe: false,
        pkceMethod: 'S256'
      });
      console.log('Keycloak initialized successfully, authenticated:', authenticated);
      this.initializedSubject.next(true);
      this.authSubject.next(authenticated);

      if (authenticated) {
        console.log('Token after init:', this.keycloak.token);
        this.updateUserProfile();
        this.setupTokenRefresh();
      }
    } catch (error) {
      console.error('Keycloak initialization failed:', error || 'No additional error details available');
      console.error('Keycloak instance state:', this.keycloak);
      this.handleInitializationError();
    }
  }

  private updateUserProfile(): void {
    try {
      if (!this.keycloak.token) {
        console.warn('No token available to decode');
        return;
      }
      const decoded = jwtDecode<DecodedToken>(this.keycloak.token);
      console.log('Decoded token:', decoded);
      this.userProfile.next(decoded);
    } catch (error) {
      console.error('Error decoding token:', error);
      this.userProfile.next({});
    }
  }

  private setupTokenRefresh(): void {
    this.refreshInterval = window.setInterval(() => {
      this.keycloak.updateToken(30)
        .then((refreshed: boolean) => {
          if (refreshed) {
            console.log('Token refreshed, new token:', this.keycloak.token);
            this.updateUserProfile();
          } else {
            console.log('Token still valid, no refresh needed');
          }
        })
        .catch((error) => {
          console.error('Token refresh failed:', error);
          this.logout();
        });
    }, 30000);
  }

  private handleInitializationError(): void {
    console.log('Handling initialization error, redirecting to /error');
    this.initializedSubject.next(true);
    this.authSubject.next(false);
    this.router.navigate(['/error']);
  }

  loadUserProfile(forceReload: boolean = false): Promise<KeycloakProfile> {
    return this.keycloak.loadUserProfile();
  }
}