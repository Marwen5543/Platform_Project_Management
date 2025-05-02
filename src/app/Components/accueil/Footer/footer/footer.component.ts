import { Component } from '@angular/core';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [],
  templateUrl: './footer.component.html',
  styleUrl: './footer.component.css'
})
export class FooterComponent {
  currentYear: number = new Date().getFullYear();
  
  // You can add methods here if needed, such as:
  subscribeToNewsletter(email: string): void {
    // Logic to handle newsletter subscription
    console.log('Subscribing email:', email);
    // Implement actual subscription logic or API call
  }
}
