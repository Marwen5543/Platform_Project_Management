// keycloak.service.ts
import { Injectable } from '@angular/core';
import Keycloak from 'keycloak-js';
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
export class KeycloakService  {
  private keycloak: any;
  private authSubject = new BehaviorSubject<boolean>(false);
  private initializedSubject = new BehaviorSubject<boolean>(false);
  private userProfile = new BehaviorSubject<DecodedToken>({});
  private refreshInterval?: number;

  constructor(private router: Router) {
    this.keycloak = new (Keycloak as any)(keycloakConfig);
    this.initializeAuth();
  }

  // Public API Methods
  getUsername(): string {
    return this.userProfile.value.preferred_username || 'Utilisateur';
  }

  getUserId(): string {
    return this.userProfile.value.sub || '';
  }

  getEmail(): string {
    return this.userProfile.value.email || '';
  }

  getRole(): string {
    const roles = this.userProfile.value.realm_access?.roles || ['EMPLOYEE'];
    return roles[0];
  }

  getRoles(): string[] {
    return this.userProfile.value.realm_access?.roles || ['EMPLOYEE'];
  }

  isAuthenticated(): boolean {
    return this.keycloak?.authenticated || false;
  }

  getToken(): string {
    return this.keycloak?.token || '';
  }

  isInitialized(): Observable<boolean> {
    return this.initializedSubject.asObservable();
  }

  getAuthObservable(): Observable<boolean> {
    return this.authSubject.asObservable();
  }

  // Authentication Flow Methods
  async canActivate(): Promise<boolean | UrlTree> {
    if (!this.initializedSubject.value) {
      await this.initializedSubject.toPromise();
    }
    return this.isAuthenticated() || this.router.parseUrl('/login');
  }

  login(): void {
    this.keycloak.login();
  }

  logout(): void {
    if (this.refreshInterval) {
      window.clearInterval(this.refreshInterval);
    }
    this.keycloak.logout({ redirectUri: window.location.origin });
  }

  // Private Implementation
  private async initializeAuth(): Promise<void> {
    try {
      const authenticated = await this.keycloak.init({
        onLoad: 'login-required',
        checkLoginIframe: false,
        pkceMethod: 'S256'
      });

      this.initializedSubject.next(true);
      this.authSubject.next(authenticated);

      if (authenticated) {
        this.updateUserProfile();
        this.setupTokenRefresh();
      }
    } catch (error) {
      console.error('Keycloak initialization failed:', error);
      this.handleInitializationError();
    }
  }

  private updateUserProfile(): void {
    try {
      const decoded = jwtDecode<DecodedToken>(this.keycloak.token);
      console.log("Decoded token:", decoded); // Log the token content
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
            this.updateUserProfile();
            console.log('Token refreshed');
          }
        })
        .catch(() => this.logout());
    }, 30000);
  }

  private handleInitializationError(): void {
    this.initializedSubject.next(true);
    this.authSubject.next(false);
    this.router.navigate(['/error']);
  }

  loadUserProfile(forceReload: boolean = false): Promise<Keycloak.KeycloakProfile> {
    return new Promise((resolve, reject) => {
      if (this.keycloak && this.keycloak.authenticated) {
        this.keycloak.loadUserProfile()
          .then((profile: Keycloak.KeycloakProfile) => resolve(profile))
          .catch((error: any) => reject(error));
      } else {
        reject('User not authenticated');
      }
    });
  }
  
}
