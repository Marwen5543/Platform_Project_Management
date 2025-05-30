import { Injectable } from '@angular/core';
import Keycloak, { KeycloakProfile } from 'keycloak-js';
import { keycloakConfig } from '../Config/keycloak-config';
import { BehaviorSubject, Observable, catchError, from, of, tap, throwError } from 'rxjs';
import { jwtDecode } from 'jwt-decode';
import { Router } from '@angular/router';
import { UserDTO, UserRole, UserStatus } from '../Models/user.models';
import { HttpClient, HttpParams } from '@angular/common/http';

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
  private authSubject = new BehaviorSubject<boolean | null>(null);
  private initializedSubject = new BehaviorSubject<boolean>(false);
  private userProfile = new BehaviorSubject<DecodedToken>({});
  private refreshInterval?: number;
  private apiUrl = 'http://localhost:8085/api';

  constructor(
    private router: Router,
    private http: HttpClient
  ) {
    console.log('Keycloak config:', keycloakConfig);
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

  getRole(): string {
    if (!this.isAuthenticated()) {
      console.warn('Keycloak not authenticated');
      return '';
    }
    const roles = this.keycloak.realmAccess?.roles || [];
    console.log('All roles from token:', roles);
    if (roles.includes('SUPER_ADMIN')) return 'SUPER_ADMIN';
    if (roles.includes('ADMIN')) return 'ADMIN';
    if (roles.includes('MANAGER')) return 'MANAGER';
    if (roles.includes('HR')) return 'HR';
    return 'EMPLOYEE';
  }

  getRoles(): string[] {
    return this.keycloak.realmAccess?.roles || [];
  }

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

  getAuthObservable(): Observable<boolean | null> {
    return this.authSubject.asObservable();
  }

  async canActivate(): Promise<boolean> {
    if (!this.initializedSubject.value) {
      await new Promise(resolve => {
        this.initializedSubject.subscribe(initialized => {
          if (initialized) resolve(true);
        });
      });
    }
    if (!this.isAuthenticated()) {
      console.log('canActivate: Not authenticated, redirecting to Keycloak login');
      this.login();
      return false;
    }
    return true;
  }

  login(): Promise<void> {
    console.log('Initiating Keycloak login');
    // Preserve the current URL for redirect after login
    const currentUrl = this.router.url;
    return this.keycloak.login({
      redirectUri: window.location.origin + (currentUrl.startsWith('/video-call') ? currentUrl : '/acceuil')
    });
  }

  logout(): Promise<void> {
    if (this.refreshInterval) {
      window.clearInterval(this.refreshInterval);
    }
    console.log('Initiating Keycloak logout');
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
          onLoad: 'check-sso',
          checkLoginIframe: false,
          pkceMethod: 'S256',
          redirectUri: window.location.origin + this.router.url
        });
        console.log('Keycloak initialized, authenticated:', authenticated);
        this.initializedSubject.next(true);
        this.authSubject.next(authenticated);

        if (authenticated) {
          console.log('Token after init:', this.keycloak.token);
          this.updateUserProfile();
          this.setupTokenRefresh();
        } else if (!this.router.url.startsWith('/video-call')) {
          console.log('Not authenticated, redirecting to Keycloak login');
          this.login();
        }
      } catch (error) {
        console.error('Keycloak initialization failed:', error);
        retries++;
        if (retries < maxRetries) {
          console.log(`Retrying initialization (${retries}/${maxRetries})...`);
          setTimeout(tryInit, retryDelay);
        } else {
          console.error('Max retries reached, redirecting to Keycloak login');
          this.initializedSubject.next(true);
          this.authSubject.next(false);
          if (!this.router.url.startsWith('/video-call')) {
            this.login();
          }
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
          this.login();
        });
    }, 30000);
  }

  loadUserProfile(forceReload: boolean = false): Promise<KeycloakProfile> {
    return this.keycloak.loadUserProfile();
  }

  refreshUserSession(): Observable<boolean> {
    console.log('Attempting to refresh user session...');
    return from(this.keycloak.updateToken(30)).pipe(
      tap(refreshed => {
        if (refreshed) {
          console.log('Token refreshed successfully');
          this.keycloak.loadUserProfile();
        } else {
          console.log('Token is still valid, no refresh needed');
        }
      }),
      catchError(error => {
        console.error('Error refreshing token:', error);
        return of(false);
      })
    );
  }

  refreshUserAttributes(): Observable<any> {
    const userId = this.getUserId();
    if (!userId) {
      console.warn('Cannot refresh attributes: No user ID found');
      return of(null);
    }
    const params = new HttpParams().set('_t', Date.now().toString());
    console.log('Fetching latest user attributes...');
    return this.http.get(`${this.apiUrl}/users/${userId}/attributes`, { params }).pipe(
      tap(attributes => console.log('Refreshed user attributes:', attributes)),
      catchError(error => {
        console.error('Error refreshing user attributes:', error);
        return of(null);
      })
    );
  }
}