import { Injectable } from '@angular/core';
import Keycloak, { KeycloakProfile } from 'keycloak-js';
import { keycloakConfig } from '../Config/keycloak-config';
import { BehaviorSubject, Observable, from, of, throwError } from 'rxjs';
import { jwtDecode } from 'jwt-decode';
import { Router, UrlTree } from '@angular/router';
import { UserDTO, UserRole, UserStatus } from '../Models/user.models';

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

  constructor(private router: Router,) {
    console.log('Keycloak config:', keycloakConfig); // Log config at construction
    console.log('Keycloak URL:', keycloakConfig.url); // Should be http://localhost:8080
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

// In keycloak.service.ts
getCurrentUser(): Observable<UserDTO> {
  if (!this.isAuthenticated() || !this.keycloak.token) {
    return throwError(() => new Error('User not authenticated'));
  }

  const decoded = jwtDecode<DecodedToken>(this.keycloak.token);
  const user: UserDTO = {
    userId: decoded.sub || '',
    username: decoded.preferred_username || 'Unknown',
    email: decoded.email || '',
    role: this.getRole() as UserRole,
    roles: this.getRoles(),
    status: UserStatus.ACTIVE, 
    isOnline: false,
    emailVerified: false,
    twoFactorEnabled: false,
    projectTitles: [],
    joinDate: new Date().toISOString() 
  };
  return of(user);
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
    if (!this.isAuthenticated()) {
      if (this.router.url === '/login') {
        return false;
      }
      return this.router.parseUrl('/login');
    }
    return true;
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
    let retries = 0;
    const maxRetries = 3;
    const retryDelay = 2000;
  
    const tryInit = async () => {
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
        console.error('Keycloak initialization failed:', error);
        console.error('Keycloak instance state:', this.keycloak);
        retries++;
        if (retries < maxRetries) {
          console.log(`Retrying initialization (${retries}/${maxRetries})...`);
          setTimeout(tryInit, retryDelay);
        } else {
          console.error('Max retries reached, handling initialization error');
          this.handleInitializationError();
        }
      }
    };
  
    await tryInit();
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
    let refreshAttempts = 0;
    const maxRefreshAttempts = 3;
  
    this.refreshInterval = window.setInterval(() => {
      this.keycloak.updateToken(30)
        .then((refreshed: boolean) => {
          refreshAttempts = 0;
          if (refreshed) {
            console.log('Token refreshed, new token:', this.keycloak.token);
            this.updateUserProfile();
          } else {
            console.log('Token still valid, no refresh needed');
          }
        })
        .catch((error) => {
          console.error('Token refresh failed:', error);
          refreshAttempts++;
          if (refreshAttempts >= maxRefreshAttempts) {
            console.warn('Max refresh attempts reached, stopping refresh and redirecting to login');
            window.clearInterval(this.refreshInterval);
            this.router.navigate(['/login'], { queryParams: { sessionExpired: true } });
          }
        });
    }, 30000);
  }

  private handleInitializationError(): void {
    console.log('Handling initialization error, redirecting to /login with error message');
    this.initializedSubject.next(true);
    this.authSubject.next(false);
    this.router.navigate(['/login'], { queryParams: { error: 'auth-failed' } });
  }

  loadUserProfile(forceReload: boolean = false): Promise<KeycloakProfile> {
    return this.keycloak.loadUserProfile();
  }
}