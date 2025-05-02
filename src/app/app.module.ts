import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { FormsModule } from '@angular/forms';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { RouterModule } from '@angular/router';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthInterceptor } from './interceptors/auth.interceptor';
import { CommonModule } from '@angular/common';

// ✅ Correction des chemins après renommage
import { HeaderComponent } from './Components/accueil/header/header.component'; // Import HeaderComponent
import { ProfileComponent } from './Components/accueil/profile/profile.component';
import { MainLayoutComponent } from './Components/accueil/main-layout/main-layout.component';
import { ManageUsersComponent } from './Components/accueil/Admin/manage-users/manage-users.component';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { FooterComponent } from './Components/accueil/Footer/footer/footer.component';

// ✅ Importation correcte du composant standalone AccueilComponent

@NgModule({
  declarations: [
    AppComponent,
    MainLayoutComponent, 
  
    
  ],
  imports: [
    BrowserModule,
    FormsModule,
    HttpClientModule,
    RouterModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    MatSnackBarModule,
    CommonModule,
    ManageUsersComponent,
    ProfileComponent,
    HeaderComponent, 
    MatProgressSpinnerModule,
    CommonModule,
    MatTableModule,
    FooterComponent
  ],
  providers: [{
    provide: HTTP_INTERCEPTORS,
    useClass: AuthInterceptor,
    multi: true
  }],
  bootstrap: [AppComponent]
})
export class AppModule { }