import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable, throwError, from, of } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { UserDTO, LoginRequest, LoginResponse, UserRole, UserStatus } from '../Models/user.models';
import { KeycloakService } from './KeycloakService';
import { MatSnackBar } from '@angular/material/snack-bar';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private apiUrl = 'http://localhost:8085/api/users';

  constructor(
    private http: HttpClient,
    private keycloakService: KeycloakService,
    private snackBar: MatSnackBar
  ) {}

  private async getAuthHeaders(): Promise<HttpHeaders> {
    const token = await this.keycloakService.getToken();
    if (!token) {
      throw new Error('No authentication token available');
    }
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  getAllUsers(): Observable<UserDTO[]> {
    return from(this.getAuthHeaders()).pipe(
      switchMap(headers => this.http.get<UserDTO[]>(this.apiUrl, { headers })),
      map(users => users.map(user => ({
        ...user,
        role: this.validateRole(user.role)
      }))),
      catchError(this.handleError.bind(this))
    );
  }

  getCurrentUserRoles(): Observable<{ roles: string[] }> {
    return from(this.getAuthHeaders()).pipe(
      switchMap(headers => this.http.get<{ roles: string[] }>(`${this.apiUrl}/myRole`, { headers })),
      catchError(this.handleError.bind(this))
    );
  }

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap((response: LoginResponse) => {
        localStorage.setItem('accessToken', response.accessToken);
        this.snackBar.open('Login successful', 'Close', { duration: 3000 });
      }),
      catchError(this.handleError.bind(this))
    );
  }

  register(userData: Partial<UserDTO>): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, userData).pipe(
      tap(() => this.snackBar.open('User registered successfully', 'Close', { duration: 3000 })),
      catchError(this.handleError.bind(this))
    );
  }

  changeUserRole(userId: string, newRole: UserRole): Observable<string> { // <---- Observable<string>
    if (!Object.values(UserRole).includes(newRole)) {
      return throwError(() => new Error('Invalid role specified'));
    }
    return from(this.getAuthHeaders()).pipe(
      switchMap(headers => this.http.put<string>( // Keep put<string>
        `${this.apiUrl}/${userId}/role`,
        { role: newRole },
        { headers, responseType: 'text' as 'json' }
      )),
      tap(() => this.snackBar.open('Role updated successfully', 'Close', { duration: 3000 })),
      // Removed this line: tap(() => this.loadUserDetails()), // REMOVE this line from service
      catchError(this.handleError.bind(this))
    );
  }

  getUserDetails(username: string): Observable<UserDTO> {
    return from(this.getAuthHeaders()).pipe(
      switchMap(headers => this.http.get<UserDTO>(`${this.apiUrl}/details/${username}`, { headers })),
      map(user => ({
        ...user,
        role: this.validateRole(user.role as string),
        roles: user.roles || [], // Ensure roles is an array
        isOnline: user.isOnline ?? false, // Default to false if not provided
        emailVerified: user.emailVerified ?? false, // Default to false
        twoFactorEnabled: user.twoFactorEnabled ?? false, // Default to false
        joinDate: user.joinDate || user.hireDate // Fallback to hireDate if joinDate is not provided
      })),
      catchError(this.handleError.bind(this))
    );
  }

  private validateRole(role: string | undefined): UserRole {
    const validRoles = Object.values(UserRole);
    const roleUpper = role?.toUpperCase();
    return roleUpper && validRoles.includes(roleUpper as UserRole) ? roleUpper as UserRole : UserRole.EMPLOYEE;
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'An unknown error occurred';
    if (error.error instanceof ErrorEvent) {
      errorMessage = `Error: ${error.error.message}`;
    } else {
      errorMessage = error.error?.message || error.message;
      switch (error.status) {
        case 401:
          if (this.keycloakService.isAuthenticated()) {
            this.keycloakService.logout(); // Logout instead of login loop
            errorMessage = 'Session expired. Logging out...';
          } else {
            this.keycloakService.login();
            errorMessage = 'Session expired. Redirecting to login...';
          }
          break;
        case 403:
          errorMessage = 'You do not have permission to perform this action';
          break;
        case 404:
          errorMessage = 'Resource not found';
          break;
        case 500:
          errorMessage = 'Server error occurred';
          break;
      }
    }
    this.snackBar.open(errorMessage, 'Close', { duration: 5000 });
    return throwError(() => new Error(errorMessage));
  }



  deleteUser(userId: string): Observable<string> { // <---- Observable<string> for success message
    return from(this.getAuthHeaders()).pipe(
        switchMap(headers => this.http.delete<string>( // Use http.delete and expect text response
            `${this.apiUrl}/${userId}`, // Assuming your backend API endpoint is /api/users/{userId} for DELETE
            { headers, responseType: 'text' as 'json' } // Expect text response
        )),
        tap(() => this.snackBar.open('User deleted successfully', 'Close', { duration: 3000 })),
        catchError(this.handleError.bind(this))
    );
}

isRoleAssignable(role: UserRole): boolean {
  return role !== UserRole.SUPER_ADMIN;
}


getUserDetailsById(userId: string): Observable<UserDTO> {
  return from(this.getAuthHeaders()).pipe(
    switchMap(headers => {
      const params = new HttpParams().set('_t', Date.now().toString());
      const noCacheHeaders = headers.append('Cache-Control', 'no-cache, no-store, must-revalidate')
                                   .append('Pragma', 'no-cache')
                                   .append('Expires', '0');
      return this.http.get<UserDTO>(`${this.apiUrl}/${userId}`, { headers: noCacheHeaders, params });
    }),
    map(user => ({
      ...user,
      role: this.validateRole(user.role as string),
      roles: user.roles || [],
      projectTitles: user.projectTitles ?? [],
      position: user.position ?? '',
      isOnline: user.isOnline ?? false,
      emailVerified: user.emailVerified ?? false,
      twoFactorEnabled: user.twoFactorEnabled ?? false,
      joinDate: user.joinDate || user.hireDate
    })),
    catchError(this.handleError.bind(this))
  );
}

updateProfile(userId: string, userData: Partial<UserDTO>): Observable<UserDTO> {
  return from(this.getAuthHeaders()).pipe(
    switchMap(headers => this.http.put<UserDTO>(
      `${this.apiUrl}/profile/${userId}`,
      userData,
      { headers }
    )),
    map(user => ({
      ...user,
      role: this.validateRole(user.role as string),
      roles: user.roles || [],
      isOnline: user.isOnline ?? false,
      emailVerified: user.emailVerified ?? false,
      twoFactorEnabled: user.twoFactorEnabled ?? false,
      joinDate: user.joinDate || user.hireDate
    })),
    tap(() => this.snackBar.open('Profile updated successfully', 'Close', { duration: 3000 })),
    catchError(this.handleError.bind(this))
  );
}
changePassword(userId: string, currentPassword: string, newPassword: string): Observable<string> {
  return from(this.getAuthHeaders()).pipe(
    switchMap(headers => this.http.post<string>(
      `${this.apiUrl}/change-password`,
      { currentPassword, newPassword },
      { headers, responseType: 'text' as 'json' }
    )),
    tap(() => this.snackBar.open('Password changed successfully', 'Close', { duration: 3000 })),
    catchError(this.handleError.bind(this))
  );
}



updateUser(user: UserDTO): Observable<UserDTO> {
  return from(this.getAuthHeaders()).pipe(
    switchMap(headers => this.http.put<UserDTO>(
      `${this.apiUrl}/${user.userId}`,
      user,
      { headers }
    )),
    map(updatedUser => ({
      ...updatedUser,
      role: this.validateRole(updatedUser.role as string),
      roles: updatedUser.roles || [],
      isOnline: updatedUser.isOnline ?? false,
      emailVerified: updatedUser.emailVerified ?? false,
      twoFactorEnabled: updatedUser.twoFactorEnabled ?? false,
      joinDate: updatedUser.joinDate || updatedUser.hireDate
    })),
    tap(() => this.snackBar.open('User updated successfully', 'Close', { duration: 3000 })),
    catchError(this.handleError.bind(this))
  );
}




removeUserFromProject(userId: string, projectTitle: string): Observable<UserDTO> {
  const savedData = localStorage.getItem(`user_${userId}_projects`);
  let user: UserDTO = {
    userId,
    username: '',
    email: '',
    role: UserRole.EMPLOYEE,
    status: UserStatus.ACTIVE, 
    projectTitles: []
  };
  if (savedData) {
    try {
      const data = JSON.parse(savedData);
      user.projectTitles = Array.isArray(data.projectTitles)
        ? data.projectTitles.filter((title: string) => title !== projectTitle)
        : [];
      user.position = typeof data.position === 'string' ? data.position : '';
      localStorage.setItem(
        `user_${userId}_projects`,
        JSON.stringify({ projectTitles: user.projectTitles, position: user.position })
      );
    } catch (e) {
      console.error(`Error updating local storage for user ${userId}:`, e);
    }
  }
  return of(user);
}


// Get the current user's name and role
  getCurrentUserNameAndRole(): Observable<{name: string, role: string}> {
    return this.http.get<{name: string, role: string}>(`${this.apiUrl}/me/name-and-role`);
  }

  // Get name and role by user ID
  getUserNameAndRole(userId: string): Observable<{name: string, role: string}> {
    return this.http.get<{name: string, role: string}>(`${this.apiUrl}/${userId}/name-and-role`);
  }

  // Get name and role by username
  getUserNameAndRoleByUsername(username: string): Observable<{name: string, role: string}> {
    return this.http.get<{name: string, role: string}>(`${this.apiUrl}/username/${username}/name-and-role`);
  }
  

assignProject(userId: string, projectTitle: string): Observable<UserDTO> {
    const encodedProjectTitle = encodeURIComponent(projectTitle);
    
    // Force cache busting to get fresh data
    const params = new HttpParams().set('_t', Date.now().toString());
    
    // Set headers to avoid caching
    const headers = new HttpHeaders({
      'Cache-Control': 'no-cache, no-store, must-revalidate, post-check=0, pre-check=0',
      'Pragma': 'no-cache',
      'Expires': '0'
    });
    
    return this.http.post<UserDTO>(
      `${this.apiUrl}/${userId}/project/${encodedProjectTitle}`, 
      {}, 
      { params, headers }
    ).pipe(
      tap(updatedUser => {
        console.log(`Project ${projectTitle} assigned to user ${userId}:`, updatedUser);
        console.log('Updated projectTitles:', updatedUser.projectTitles);
      }),
      catchError(error => {
        console.error(`Error assigning project ${projectTitle} to user ${userId}:`, error);
        throw error;
      })
    );
  }

deassignProject(userId: string, projectTitle: string): Observable<UserDTO> {
    const encodedProjectTitle = encodeURIComponent(projectTitle);
    
    // Force cache busting to get fresh data
    const params = new HttpParams().set('_t', Date.now().toString());
    
    // Set headers to avoid caching
    const headers = new HttpHeaders({
      'Cache-Control': 'no-cache, no-store, must-revalidate, post-check=0, pre-check=0',
      'Pragma': 'no-cache',
      'Expires': '0'
    });
    
    return this.http.delete<UserDTO>(
      `${this.apiUrl}/${userId}/project/${encodedProjectTitle}`, 
      { params, headers }
    ).pipe(
      tap(updatedUser => {
        console.log(`Project ${projectTitle} deassigned from user ${userId}:`, updatedUser);
        console.log('Updated projectTitles:', updatedUser.projectTitles);
      }),
      catchError(error => {
        console.error(`Error deassigning project ${projectTitle} from user ${userId}:`, error);
        throw error;
      })
    );
  }

}
