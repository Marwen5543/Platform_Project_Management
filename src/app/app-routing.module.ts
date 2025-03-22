import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AcceuilComponent } from './Components/acceuil/acceuil.component';
import { ProfileComponent } from './Components/acceuil/profile/profile.component';
import { KeycloakService } from './Service/KeycloakService';
import { MainLayoutComponent } from './Components/acceuil/main-layout/main-layout.component';

const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [KeycloakService],
    children: [
      { path: 'acceuil', component: AcceuilComponent },
      { path: 'profile', component: ProfileComponent },
      { path: '', redirectTo: 'acceuil', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: 'acceuil' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
