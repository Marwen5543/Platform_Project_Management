import { Component, OnInit, ChangeDetectorRef, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserDTO, UserRole, UserStatus } from 'src/app/Models/user.models';
import { KeycloakService } from 'src/app/Service/KeycloakService';
import { UserService } from 'src/app/Service/UserService';
import { Router } from '@angular/router';





@Component({
  selector: 'app-acceuil',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './acceuil.component.html',
  styleUrls: ['./acceuil.component.css']
})
export class AcceuilComponent implements AfterViewInit, OnDestroy {
  
  emailAddress: string = 'contact@tunisys.com.tn';
 
  private slideData = [
    {
      title: 'Notre force est notre capital humain qui continue sans cesse de se développer',
      description: "Depuis 1981, les équipes de Tunisys innovent en permanence et développent des compétences et expertises de très haut niveau, pour concevoir les architectures et les services les mieux adaptés à chaque organisation.",
      cardIconClass: 'fas fa-users',
      cardTitle: 'Capital Humain',
      cardText: "Plus de 40 ans d'expérience dans l'innovation et le développement de solutions technologiques de pointe."
    },
    {
      title: 'Des Partenariats Stratégiques pour des Solutions Complètes',
      description: "Nous nous appuyons sur les meilleures technologies du marché, en partenariat avec des leaders mondiaux comme Huawei, HP, et Dell, pour offrir des solutions globales et parfaitement adaptées à vos besoins.",
      cardIconClass: 'fas fa-handshake',
      cardTitle: 'Partenaires Leaders',
      cardText: "Accédez à un écosystème technologique de premier plan pour garantir la performance et la fiabilité de vos infrastructures."
    },
    {
      title: 'L\'Innovation au Cœur de Votre Transformation Digitale',
      description: "Notre démarche est proactive : nous anticipons les évolutions technologiques pour vous proposer des solutions avant-gardistes qui vous garantissent un avantage concurrentiel durable.",
      cardIconClass: 'fas fa-lightbulb',
      cardTitle: 'Innovation Continue',
      cardText: "De la sécurité Zero Trust au Cloud Hybride, nous maîtrisons les concepts qui dessinent le futur de l'IT."
    }
  ];

  private currentIndex = 0;
  private slideInterval: any; // We will use this to store our auto-play timer

  constructor() {}

  ngAfterViewInit(): void {
    this.setupSlider();
    // NEW: Start the auto-play as soon as the component is ready
    this.startAutoPlay(15000); // 15000 milliseconds = 15 seconds
  }

  ngOnDestroy(): void {
    // Clean up the timer when the component is destroyed to prevent memory leaks
    if (this.slideInterval) {
      clearInterval(this.slideInterval);
    }
  }

  private setupSlider(): void {
    const nextBtn = document.getElementById('nextBtn');
    const prevBtn = document.getElementById('prevBtn');
    const indicators = document.querySelectorAll('.indicator');

    if (nextBtn) {
      nextBtn.addEventListener('click', () => {
        this.nextSlide();
        this.resetAutoPlay(); // NEW: Reset the timer on manual click
      });
    }
    if (prevBtn) {
      prevBtn.addEventListener('click', () => {
        this.prevSlide();
        this.resetAutoPlay(); // NEW: Reset the timer on manual click
      });
    }

    indicators.forEach(indicator => {
      indicator.addEventListener('click', (e) => {
        const slideIndex = (e.currentTarget as HTMLElement).dataset['slide'];
        if (slideIndex) {
          this.goToSlide(parseInt(slideIndex, 10));
          this.resetAutoPlay(); // NEW: Reset the timer on manual click
        }
      });
    });
  }

  private nextSlide(): void {
    const nextIndex = (this.currentIndex + 1) % this.slideData.length;
    this.updateSlideContent(nextIndex);
  }

  private prevSlide(): void {
    const prevIndex = (this.currentIndex - 1 + this.slideData.length) % this.slideData.length;
    this.updateSlideContent(prevIndex);
  }

  private goToSlide(index: number): void {
    if (index !== this.currentIndex) {
      this.updateSlideContent(index);
    }
  }

  private updateSlideContent(newIndex: number): void {
    const contentElements = [
      document.getElementById('heroTitle'),
      document.getElementById('heroDesc'),
      document.querySelector('.floating-card')
    ];

    contentElements.forEach(el => el?.classList.add('content-fading-out'));
    
    setTimeout(() => {
      this.currentIndex = newIndex;
      const data = this.slideData[this.currentIndex];

      const heroTitle = document.getElementById('heroTitle');
      const heroDesc = document.getElementById('heroDesc');
      if(heroTitle) heroTitle.innerHTML = data.title;
      if(heroDesc) heroDesc.innerText = data.description;
      
      const cardIcon = document.getElementById('cardIcon');
      const cardTitle = document.getElementById('cardTitle');
      const cardText = document.getElementById('cardText');
      if(cardIcon) cardIcon.className = `fas ${data.cardIconClass}`;
      if(cardTitle) cardTitle.innerText = data.cardTitle;
      if(cardText) cardText.innerText = data.cardText;

      const slides = document.querySelectorAll('.hero-slide');
      slides.forEach((slide, index) => {
        slide.classList.toggle('active', index === this.currentIndex);
      });

      const indicators = document.querySelectorAll('.indicator');
      indicators.forEach((indicator, index) => {
        indicator.classList.toggle('active', index === this.currentIndex);
      });

      contentElements.forEach(el => el?.classList.remove('content-fading-out'));

    }, 350);
  }

  // --- NEW: AUTO-PLAY FUNCTIONALITY ---
  private startAutoPlay(duration: number): void {
    // Set an interval that calls nextSlide every 'duration' milliseconds
    this.slideInterval = setInterval(() => {
      this.nextSlide();
    }, duration);
  }

  private resetAutoPlay(): void {
    // Clear the existing timer
    clearInterval(this.slideInterval);
    // Restart it immediately with the same duration
    this.startAutoPlay(15000);
  }
}