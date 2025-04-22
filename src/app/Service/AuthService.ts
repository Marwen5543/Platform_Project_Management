import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, from, map } from 'rxjs';
import { CreateUserRequest, UserDTO } from '../Models/user.models';
import { KeycloakService } from './KeycloakService';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8085/api/users';

  constructor(
    private http: HttpClient,
    private keycloakService: KeycloakService
  ) {}

  register(user: CreateUserRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, user);
  }

  login(): Observable<void> {
    // Use Keycloak for login
    return from(this.keycloakService.login()).pipe(
      map(() => void 0)  // Return void since KeycloakService.login() redirects
    );
  }

  getCurrentUser(): Observable<UserDTO> {
    return this.http.get<UserDTO>(`${this.apiUrl}/me`);
  }

  logout(): Observable<void> {
    // Use Keycloak for logout
    return from(this.keycloakService.logout()).pipe(
      map(() => void 0)  // Return void since KeycloakService.logout() redirects
    );
  }
}