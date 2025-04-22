import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor
} from '@angular/common/http';
import { catchError, from, Observable, switchMap, throwError } from 'rxjs';
import { KeycloakService } from '../Service/KeycloakService';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private keycloakService: KeycloakService) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    if (this.keycloakService.isAuthenticated()) {
      return from(this.keycloakService.getToken()).pipe(
        switchMap(token => {
          console.log('Intercepting request with token:', token);
          const authReq = request.clone({
            setHeaders: {
              Authorization: `Bearer ${token}`
            }
          });
          return next.handle(authReq);
        }),
        catchError(error => {
          console.error('Error fetching token for request:', error);
          return throwError(() => error);
        })
      );
    }
    console.log('No authentication, sending request without token');
    return next.handle(request);
  }
}