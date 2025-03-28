import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators'; // Import tap operator
import { UserDTO, LoginRequest, LoginResponse } from '../Models/user.models';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private apiUrl = 'http://localhost:8085/api/users'; 

  constructor(private http: HttpClient) {}

  // Fetch all users
  getAllUsers(): Observable<UserDTO[]> {
    return this.http.get<UserDTO[]>(this.apiUrl);
  }

  // Fetch the current logged-in user
  getDatabaseRole(): Observable<{ databaseRole: string }> {
    return this.http.get<{ databaseRole: string }>(`${this.apiUrl}/myRole`);
  }

  // Login user
  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap((response: LoginResponse) => {
        localStorage.setItem('accessToken', response.accessToken); // Save token locally
      })
    );
  }

  // Check if user is authenticated
  isAuthenticated(): boolean {
    return !!localStorage.getItem('accessToken');
  }

  // Register a new user
  register(userData: Partial<UserDTO>): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, userData);
  }

  // Change user role (admin-only)
  changeUserRole(userId: string, newRole: string): Observable<void> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/json'
      // Add Authorization header if using Keycloak/JWT
      // 'Authorization': `Bearer ${yourTokenHere}`
    });
    const body = { role: newRole };
    return this.http.put<void>(`${this.apiUrl}/${userId}/role`, body, { headers });
  }

  getUserDetails(username: string): Observable<UserDTO> {
    return this.http.get<UserDTO>(`${this.apiUrl}/details/${username}`);
  }
}
