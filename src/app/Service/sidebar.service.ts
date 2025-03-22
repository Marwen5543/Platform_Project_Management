import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, debounceTime, fromEvent } from 'rxjs';

// sidebar.service.ts
@Injectable({ providedIn: 'root' })
export class SidebarService {
  // State subjects
  private collapsedState = new BehaviorSubject<boolean>(false);
  private selectedSection = new BehaviorSubject<string>('acceuil');
  private showGestionConge = new BehaviorSubject<boolean>(false);
  private showDocuments = new BehaviorSubject<boolean>(false);
  private isMobileState = new BehaviorSubject<boolean>(false);

  // Observables
  collapsed$ = this.collapsedState.asObservable();
  selectedSection$ = this.selectedSection.asObservable();
  showGestionConge$ = this.showGestionConge.asObservable();
  showDocuments$ = this.showDocuments.asObservable();
  isMobile$ = this.isMobileState.asObservable();

  constructor(private router: Router) {
    this.initState();
    this.setupMobileCheck();
  }

  private initState() {
    const savedState = localStorage.getItem('sidebarCollapsed');
    if (savedState) this.collapsedState.next(JSON.parse(savedState));
  }

  private setupMobileCheck() {
    window.addEventListener('resize', () => this.checkMobile());
    this.checkMobile();
  }

  checkMobile() {
    this.isMobileState.next(window.innerWidth <= 768);
  }

  toggle() {
    const newState = !this.collapsedState.value;
    this.collapsedState.next(newState);
    localStorage.setItem('sidebarCollapsed', JSON.stringify(newState));
  }

  setSelectedSection(section: string) {
    this.selectedSection.next(section);
  }

  toggleGestionConge() {
    this.showGestionConge.next(!this.showGestionConge.value);
    this.showDocuments.next(false);
  }

  toggleDocuments() {
    this.showDocuments.next(!this.showDocuments.value);
    this.showGestionConge.next(false);
  }

  navigateTo(route: string) {
    this.router.navigate([route]); 
    if (this.isMobileState.value) this.collapsedState.next(true);
  }
}