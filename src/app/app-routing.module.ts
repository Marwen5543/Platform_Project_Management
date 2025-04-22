import { Component, NgModule } from '@angular/core';
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
import { TeamLeavesComponent } from './Components/accueil/team-leaves/team-leaves.component';
import { LeaveRequestComponent } from './Components/accueil/leave-request/leave-request.component';
import { LeaveHistoryComponent } from './Components/accueil/leave-history/leave-history.component';

const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [KeycloakService],
    children: [
      { path: 'profile', component: ProfileComponent },
      { path: 'manage-users', component: ManageUsersComponent },
      { path: 'system-settings', component: SystemSettingsComponent },
      { path: 'acceuil',component: AcceuilComponent},
      { path: 'project-affectation',component: ProjectAffectationComponent},
      { path: 'edit-profile/:userId', component: EditProfileComponent },
      { path: 'fiche-paie', component: DocumentRequestComponent, data: { type: 'fiche-paie' } },
      { path: 'attestation-travail', component: DocumentRequestComponent, data: { type: 'attestation-travail' } },
      { path: 'certificat-travail', component: DocumentRequestComponent, data: { type: 'certificat-travail' } },
      { path: 'hr/document-requests', component: HrDocumentRequestsComponent },
      { path: 'document-history', component: DocumentComponent},
      { path: 'demande-conge', component: LeaveRequestComponent },
      { path: 'consulter-conge', component: LeaveHistoryComponent },
      { path: 'liste-equipe-conge', component: TeamLeavesComponent },
      { path: '', redirectTo: 'profile', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: 'profile' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
