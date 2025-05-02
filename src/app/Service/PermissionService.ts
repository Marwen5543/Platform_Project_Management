import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { map, switchMap, catchError } from 'rxjs/operators';
import { KeycloakService } from './KeycloakService';
import { UserService } from './UserService';
import { UserRole } from '../Models/user.models';

@Injectable({
  providedIn: 'root'
})
export class PermissionService {
  // Define role hierarchy: SuperAdmin > Admin > Manager > HR > Employee
  private roleHierarchy: Record<UserRole, number> = {
    [UserRole.SUPER_ADMIN]: 5,
    [UserRole.ADMIN]: 4,
    [UserRole.MANAGER]: 3,
    [UserRole.HR]: 2,
    [UserRole.EMPLOYEE]: 1
  };

  constructor(
    private keycloakService: KeycloakService,
    private userService: UserService
  ) {}

  canDeleteTask(creatorId: string): Observable<boolean> {
    return this.keycloakService.getCurrentUser().pipe(
      switchMap(currentUser => {
        if (!currentUser) {
          console.log('No current user, denying delete permission');
          return of(false);
        }
        console.log(`Checking delete permission for user ${currentUser.userId}, creatorId: ${creatorId}, raw role: ${currentUser.role}`);
        // Validate creatorId
        if (!creatorId || creatorId === 'unknown') {
          console.log(`Invalid creatorId: ${creatorId}, denying delete`);
          return of(false);
        }
        // Allow deletion if the user is the creator
        if (currentUser.userId === creatorId) {
          console.log(`User ${currentUser.userId} is creator, allowing delete`);
          return of(true);
        }
        // Fetch creator's role
        return this.userService.getUserDetailsById(creatorId).pipe(
          map(creator => {
            const currentUserRole = (currentUser.role as UserRole) in this.roleHierarchy ? (currentUser.role as UserRole) : UserRole.EMPLOYEE;
            const creatorRole = (creator.role as UserRole) in this.roleHierarchy ? (creator.role as UserRole) : UserRole.EMPLOYEE;
            const currentUserRoleLevel = this.roleHierarchy[currentUserRole] || 1;
            const creatorRoleLevel = this.roleHierarchy[creatorRole] || 1;
            const canDelete = currentUserRoleLevel > creatorRoleLevel;
            console.log(`Delete permission: ${canDelete}, Current user: ${currentUser.userId} (role: ${currentUserRole}, level: ${currentUserRoleLevel}), Creator: ${creator.userId} (role: ${creatorRole}, level: ${creatorRoleLevel})`);
            return canDelete;
          }),
          catchError(err => {
            console.error(`Error fetching creator details for ${creatorId}:`, err.message || err);
            return of(false);
          })
        );
      }),
      catchError(err => {
        console.error('Error checking delete permission:', err.message || err);
        return of(false);
      })
    );
  }

  canEditTask(creatorId: string): Observable<boolean> {
    return this.keycloakService.getCurrentUser().pipe(
      switchMap(currentUser => {
        if (!currentUser) {
          console.log('No current user, denying edit permission');
          return of(false);
        }
        console.log(`Checking edit permission for user ${currentUser.userId}, creatorId: ${creatorId}, raw role: ${currentUser.role}`);
        // Validate creatorId
        if (!creatorId || creatorId === 'unknown') {
          console.log(`Invalid creatorId: ${creatorId}, denying edit`);
          return of(false);
        }
        // Allow editing if the user is the creator
        if (currentUser.userId === creatorId) {
          console.log(`User ${currentUser.userId} is creator, allowing edit`);
          return of(true);
        }
        // Fetch creator's role
        return this.userService.getUserDetailsById(creatorId).pipe(
          map(creator => {
            const currentUserRole = (currentUser.role as UserRole) in this.roleHierarchy ? (currentUser.role as UserRole) : UserRole.EMPLOYEE;
            const creatorRole = (creator.role as UserRole) in this.roleHierarchy ? (creator.role as UserRole) : UserRole.EMPLOYEE;
            const currentUserRoleLevel = this.roleHierarchy[currentUserRole] || 1;
            const creatorRoleLevel = this.roleHierarchy[creatorRole] || 1;
            const canEdit = currentUserRoleLevel > creatorRoleLevel;
            console.log(`Edit permission: ${canEdit}, Current user: ${currentUser.userId} (role: ${currentUserRole}, level: ${currentUserRoleLevel}), Creator: ${creator.userId} (role: ${creatorRole}, level: ${creatorRoleLevel})`);
            return canEdit;
          }),
          catchError(err => {
            console.error(`Error fetching creator details for ${creatorId}:`, err.message || err);
            return of(false);
          })
        );
      }),
      catchError(err => {
        console.error('Error checking edit permission:', err.message || err);
        return of(false);
      })
    );
  }

  canViewTaskDetails(taskCreatorId: string): Observable<boolean> {
    return this.keycloakService.getCurrentUser().pipe(
      switchMap(currentUser => {
        if (!currentUser) {
          console.log('No current user, denying view permission');
          return of(false);
        }
        console.log(`Checking view permission for user ${currentUser.userId}, creatorId: ${taskCreatorId}, raw role: ${currentUser.role}`);
        // Validate creatorId
        if (!taskCreatorId || taskCreatorId === 'unknown') {
          console.log(`Invalid creatorId: ${taskCreatorId}, denying view`);
          return of(false);
        }
        // Allow viewing if the user is the creator
        if (currentUser.userId === taskCreatorId) {
          console.log(`User ${currentUser.userId} is creator, allowing view`);
          return of(true);
        }
        // Fetch creator's role
        return this.userService.getUserDetailsById(taskCreatorId).pipe(
          map(creator => {
            const currentUserRole = (currentUser.role as UserRole) in this.roleHierarchy ? (currentUser.role as UserRole) : UserRole.EMPLOYEE;
            const creatorRole = (creator.role as UserRole) in this.roleHierarchy ? (creator.role as UserRole) : UserRole.EMPLOYEE;
            const currentUserRoleLevel = this.roleHierarchy[currentUserRole] || 1;
            const creatorRoleLevel = this.roleHierarchy[creatorRole] || 1;
            const canView = currentUserRoleLevel >= creatorRoleLevel;
            console.log(`View permission: ${canView}, Current user: ${currentUser.userId} (role: ${currentUserRole}, level: ${currentUserRoleLevel}), Creator: ${creator.userId} (role: ${creatorRole}, level: ${creatorRoleLevel})`);
            return canView;
          }),
          catchError(err => {
            console.error(`Error fetching creator details for ${taskCreatorId}:`, err.message || err);
            return of(false);
          })
        );
      }),
      catchError(err => {
        console.error('Error checking view permission:', err.message || err);
        return of(false);
      })
    );
  }
}
