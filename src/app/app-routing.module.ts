import { Component, NgModule, Injectable } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { KeycloakService } from './Service/KeycloakService';
import { MainLayoutComponent } from './Components/accueil/main-layout/main-layout.component';
import { ProfileComponent } from './Components/accueil/profile/profile.component';
import { ManageUsersComponent } from './Components/accueil/Admin/manage-users/manage-users.component';
import { SystemSettingsComponent } from './Components/accueil/Admin/system-settings/system-settings.component';
import { AcceuilComponent } from './Components/accueil/acceuil/acceuil/acceuil.component';
import { ProjectAffectationComponent } from './Components/accueil/Admin/project-affectation/project-affectation/project-affectation.component';
import { EditProfileComponent } from './Components/accueil/edit-profile/edit-profile.component';
import { DocumentRequestComponent } from './Components/accueil/Documents/document-request/document-request.component';
import { HrDocumentRequestsComponent } from './Components/accueil/Documents/hr-document-requests/hr-document-requests.component';
import { DocumentComponent } from './Components/accueil/Documents/document/document.component';
import { TeamLeavesComponent } from './Components/accueil/LeaveRequest/team-leaves/team-leaves.component';
import { LeaveHistoryComponent } from './Components/accueil/LeaveRequest/leave-history/leave-history.component';
import { CalanderComponent } from './Components/accueil/Calander/calander/calander.component';
import { LeaveRequestComponent } from './Components/accueil/LeaveRequest/leave-request/leave-request.component';
import { VideoCallComponent } from './Components/accueil/video-call/video-call.component';
import { ProjetsComponent } from './Components/accueil/projets/projets.component';

// Guard to explicitly allow unauthenticated access
@Injectable({
  providedIn: 'root'
})
export class NoAuthGuard {
  canActivate(): boolean {
    console.log('NoAuthGuard: Allowing access to video-call route');
    return true;
  }
}

const routes: Routes = [
  { 
    path: 'video-call/:taskId', 
    component: VideoCallComponent,
    canActivate: [NoAuthGuard]
  },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [KeycloakService],
    children: [
      { path: 'profile', component: ProfileComponent },
      { path: 'manage-users', component: ManageUsersComponent },
      { path: 'system-settings', component: SystemSettingsComponent },
      { path: 'acceuil', component: AcceuilComponent },
      { path: 'project-affectation', component: ProjectAffectationComponent },
      { path: 'edit-profile/:userId', component: EditProfileComponent },
      { path: 'fiche-paie', component: DocumentRequestComponent, data: { type: 'fiche-paie' } },
      { path: 'attestation-travail', component: DocumentRequestComponent, data: { type: 'attestation-travail' } },
      { path: 'certificat-travail', component: DocumentRequestComponent, data: { type: 'certificat-travail' } },
      { path: 'hr/document-requests', component: HrDocumentRequestsComponent },
      { path: 'document-history', component: DocumentComponent },
      { path: 'demande-conge', component: LeaveRequestComponent },
      { path: 'consulter-conge', component: LeaveHistoryComponent },
      { path: 'liste-equipe-conge', component: TeamLeavesComponent },
      { path: 'planification', component: CalanderComponent },
      { path: 'projets', component: ProjetsComponent},
      { path: '', redirectTo: 'acceuil', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: '', pathMatch: 'full' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, { enableTracing: true })],
  exports: [RouterModule]
})
export class AppRoutingModule { }