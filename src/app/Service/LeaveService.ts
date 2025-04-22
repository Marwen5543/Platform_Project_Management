import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { catchError, from, Observable, switchMap, throwError, map, tap, retry } from 'rxjs';
import { KeycloakService } from './KeycloakService';
import { LeaveRequest, LeaveRequestDto, LeaveStatus } from '../Models/LeaveRequest';
import { jwtDecode } from 'jwt-decode'; 

interface DecodedToken {
    realm_access?: {
        roles: string[];
    };
}

@Injectable({ providedIn: 'root' })
export class LeaveService {
    private apiUrl = 'http://localhost:8088/api/leaves';

    constructor(private http: HttpClient, private keycloakService: KeycloakService) {}

    private getHeaders(): Observable<HttpHeaders> {
        return from(this.keycloakService.getToken()).pipe(
            switchMap((token: string) => {
                if (!token) {
                    throw new Error('No authentication token available');
                }
                return [new HttpHeaders({
                    Authorization: `Bearer ${token}`,
                    'Content-Type': 'application/json'
                })];
            }),
            catchError(() => {
                console.error('Failed to retrieve authentication token');
                return throwError(() => new Error('Failed to retrieve authentication token'));
            })
        );
    }

    requestLeave(dto: LeaveRequestDto): Observable<LeaveRequest> {
        return this.getHeaders().pipe(
            switchMap(headers =>
                this.http.post<LeaveRequest>(`${this.apiUrl}/request`, dto, { headers })
            )
        );
    }

    getLeaveHistory(): Observable<LeaveRequest[]> {
        console.log('Fetching leave history...');
        return this.getHeaders().pipe(
            switchMap(headers =>
                this.http.get<LeaveRequest[]>(`${this.apiUrl}/history`, { headers })
            ),
            retry(1),
            map(leaves => this.sortLeavesByStatus(leaves)),
            catchError(this.handleError)
        );
    }

    getTeamLeaves(): Observable<LeaveRequest[]> {
        console.log('Fetching team leaves from:', `${this.apiUrl}/team`);
        return this.getHeaders().pipe(
            switchMap(headers =>
                this.http.get<LeaveRequest[]>(`${this.apiUrl}/team`, { headers })
            ),
            retry(1),
            tap(leaves => console.log('Team leaves fetched:', leaves)),
            catchError(this.handleError)
        );
    }

    updateLeaveStatus(leaveId: string, status: string): Observable<any> {
      return this.getHeaders().pipe(
          tap(headers => {
              const token = headers.get('Authorization')?.replace('Bearer ', '');
              console.log('Token sent in request:', token);
              if (token) {
                  try {
                      const decoded = jwtDecode<DecodedToken>(token);
                      console.log('Token roles:', decoded.realm_access?.roles || []);
                      console.log('HR role present:', decoded.realm_access?.roles?.includes('HR'));
                  } catch (error) {
                      console.error('Error decoding token:', error);
                  }
              }
          }),
          switchMap(headers =>
              this.http.put(`${this.apiUrl}/${leaveId}/status`, { status }, { headers })
          ),
          catchError(error => {
              if (error.status === 403) {
                  console.error('Permission denied: User does not have HR role');
                  alert('You do not have permission to update leave status. HR role required.');
              }
              return throwError(() => error);
          })
      );
  }

    private sortLeavesByStatus(leaves: LeaveRequest[]): LeaveRequest[] {
        return [...leaves].sort((a, b) => {
            if (a.status === LeaveStatus.PENDING && b.status !== LeaveStatus.PENDING) {
                return -1;
            }
            if (a.status !== LeaveStatus.PENDING && b.status === LeaveStatus.PENDING) {
                return 1;
            }
            const dateA = new Date(a.startDate);
            const dateB = new Date(b.startDate);
            return dateB.getTime() - dateA.getTime();
        });
    }

    private handleError(error: any) {
        console.error('An error occurred:', error);
        return throwError(() => new Error('Something went wrong. Please try again later.'));
    }
}