import { Component, ViewEncapsulation } from '@angular/core';
import { SidebarService } from 'src/app/Service/sidebar.service';
import { trigger, transition, style, animate } from '@angular/animations';

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css'],
  animations: [
    trigger('slideDown', [
      transition(':enter', [
        style({ height: 0, opacity: 0, overflow: 'hidden' }),
        animate('300ms ease-out', style({ height: '*', opacity: 1 }))
      ]),
      transition(':leave', [
        style({ overflow: 'hidden' }),
        animate('300ms ease-in', style({ height: 0, opacity: 0 }))
      ])
    ])
  ],
    encapsulation: ViewEncapsulation.None // Disable encapsulation
  
})

export class SidebarComponent {
  constructor(public sidebarService: SidebarService) {}

  navigateAndSelect(section: string, route: string) {
    this.sidebarService.setSelectedSection(section);
    this.sidebarService.navigateTo(route);
  }

  
}