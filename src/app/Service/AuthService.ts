import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, from, of, map, catchError, throwError } from 'rxjs';
import { CreateUserRequest, UserDTO } from '../Models/user.models';
import { KeycloakService } from './KeycloakService';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8085/api/users';

  constructor(
    private http: HttpClient,
    private keycloakService: KeycloakService,
    private router: Router
  ) {
    // Subscribe to auth state changes
    this.keycloakService.getAuthObservable().subscribe(authenticated => {
      if (!authenticated) {
        console.log('Auth state changed to not authenticated');
        // Maybe show a message to the user that they need to log in again
      }
    });
  }

  register(user: CreateUserRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, user);
  }

  login(): Observable<void> {
    // Use Keycloak for login
    return from(this.keycloakService.login()).pipe(
      map(() => void 0),
      catchError(error => {
        console.error('Login failed:', error);
        return throwError(() => new Error('Login failed'));
      })
    );
  }

  getCurrentUser(): Observable<UserDTO> {
    // Try to get from Keycloak first
    if (this.keycloakService.isAuthenticated()) {
      return this.keycloakService.getCurrentUser().pipe(
        catchError(error => {
          console.warn('Failed to get user from Keycloak, falling back to API:', error);
          // Fall back to API
          return this.getUserFromApi();
        })
      );
    }
    
    // Not authenticated with Keycloak
    return throwError(() => new Error('Not authenticated'));
  }

  private getUserFromApi(): Observable<UserDTO> {
    return this.http.get<UserDTO>(`${this.apiUrl}/me`).pipe(
      catchError(error => {
        console.error('Failed to get user from API:', error);
        // If API fails, redirect to login
        this.router.navigate(['/login']);
        return throwError(() => new Error('Failed to get user information'));
      })
    );
  }

  logout(): Observable<void> {
    // Use Keycloak for logout
    return from(this.keycloakService.logout()).pipe(
      map(() => void 0),
      catchError(error => {
        console.error('Logout failed:', error);
        // Even if Keycloak logout fails, redirect to login page
        this.router.navigate(['/login']);
        return of(void 0);
      })
    );
  }

  // Method to check if token is valid
  checkAuthStatus(): Observable<boolean> {
    if (!this.keycloakService.isAuthenticated()) {
      return of(false);
    }
    
    // Try to get a fresh token
    return from(this.keycloakService.getToken()).pipe(
      map(token => token !== ''),
      catchError(() => of(false))
    );
  }
}