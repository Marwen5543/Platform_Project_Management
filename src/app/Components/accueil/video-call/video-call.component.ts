import { Component, OnInit, OnDestroy, AfterViewInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { v4 as uuidv4 } from 'uuid';
import { HeaderComponent } from '../header/header.component';
import { FooterComponent } from '../Footer/footer/footer.component';

@Component({
  selector: 'app-video-call',
  standalone: true,
  imports: [CommonModule,HeaderComponent, FooterComponent],
  template: `
    <div class="app-container">
      <app-header></app-header>
      <br>
      <br><br>
      <main class="video-call-container">
        <h2>Welcome to Tunisys Video Conference</h2>

        

        <div id="jitsi-container" class="video-frame"></div>
      </main>

      <app-footer></app-footer>
    </div>
  `,
  styles: [`
    .app-container {
      display: flex;
      flex-direction: column;
      min-height: 100vh;
    }

    .video-call-container {
      flex: 1;
      padding: 2rem;
      max-width: 1000px;
      margin: 0 auto;
      text-align: center;
    }

    h2 {
      font-size: 2rem;
      margin-bottom: 1.5rem;
    }

    .share-section {
      margin-bottom: 1.5rem;
      text-align: left;
    }

    .share-link-wrapper {
      display: flex;
      gap: 0.5rem;
      align-items: center;
    }

    input[type="text"] {
      flex: 1;
      padding: 0.6rem 1rem;
      border: 1px solid #ccc;
      border-radius: 6px;
      font-size: 1rem;
    }

    button {
      padding: 0.6rem 1rem;
      font-size: 1rem;
      background-color: #007bff;
      color: #fff;
      border: none;
      border-radius: 6px;
      cursor: pointer;
      transition: background-color 0.2s;
    }

    button:hover {
      background-color: #0056b3;
    }

    .video-frame {
      margin-top: 2rem;
      width: 100%;
      height: 600px;
      border-radius: 8px;
      overflow: hidden;
      box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
    }

    @media (max-width: 768px) {
      .share-link-wrapper {
        flex-direction: column;
        align-items: stretch;
      }

      button {
        width: 100%;
      }
    }
  `]
})
export class VideoCallComponent implements OnInit, OnDestroy, AfterViewInit {
  taskId = '';
  joinLink = '';
  domain = 'meet.jit.si'; // Replace with your own Jitsi domain if self-hosted
  api: any;

  constructor(private route: ActivatedRoute, private router: Router) {}

  ngOnInit() {
    this.route.params.subscribe(params => {
      this.taskId = params['taskId'] || this.generateMeetingId();
      this.joinLink = `${window.location.origin}/video-call/${encodeURIComponent(this.taskId)}`;
    });
  }

  ngAfterViewInit(): void {
    this.startJitsiMeeting();
  }

  ngOnDestroy(): void {
    if (this.api) {
      this.api.dispose();
    }
  }

  startJitsiMeeting() {
    const options = {
      roomName: this.taskId,
      parentNode: document.getElementById('jitsi-container'),
      width: '100%',
      height: 600,
      configOverwrite: {},
      interfaceConfigOverwrite: {},
      userInfo: {
        displayName: 'Guest User'
      }
    };
    this.api = new (window as any).JitsiMeetExternalAPI(this.domain, options);
  }

  copyLink(): void {
    navigator.clipboard.writeText(this.joinLink).then(() => {
      alert('Meeting link copied!');
    });
  }

  createNewMeeting(): void {
    const newMeetingId = this.generateMeetingId();
    this.router.navigate(['/video-call', newMeetingId]);
  }

  private generateMeetingId(): string {
    return uuidv4();
  }
}
