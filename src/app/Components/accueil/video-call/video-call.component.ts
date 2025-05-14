import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ZegoUIKitPrebuilt } from '@zegocloud/zego-uikit-prebuilt';
import { KeycloakService } from 'src/app/Service/KeycloakService';

@Component({
  selector: 'app-video-call',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './video-call.component.html',
  styleUrl: './video-call.component.css',
})
export class VideoCallComponent implements OnInit, OnDestroy {
  taskId: string = '';
  userId: string = '';
  userName: string = '';
  errorMessage: string | null = null;
  private zp: any = null; // Store ZEGOCLOUD instance
  private localStream: MediaStream | null = null; // Store local media stream

  constructor(
    private route: ActivatedRoute,
    private keycloakService: KeycloakService
  ) {}

  ngOnInit() {
    // Check if running in a secure context
    if (!window.isSecureContext) {
      this.errorMessage = 'Video calls require a secure context (HTTPS or localhost).';
      console.error('Insecure context detected');
      return;
    }

    this.route.params.subscribe(params => {
      this.taskId = params['taskId'];
      this.keycloakService.getCurrentUser().subscribe({
        next: (currentUser) => {
          if (currentUser) {
            this.userId = currentUser.userId;
            this.userName = currentUser.username || this.userId;
            if (this.taskId && this.userId) {
              this.checkMediaPermissions();
            } else {
              this.errorMessage = 'Task ID or User ID missing';
              console.error('taskId or userId missing');
            }
          } else {
            this.errorMessage = 'User not authenticated';
            console.error('User not authenticated');
          }
        },
        error: (err) => {
          this.errorMessage = 'Failed to fetch current user';
          console.error('Failed to fetch current user:', err);
        }
      });
    });
  }

  ngOnDestroy() {
    // Clean up ZEGOCLOUD meeting
    if (this.zp) {
      try {
        this.zp.hangUp(); // End the meeting
        this.zp.destroy(); // Destroy the ZEGOCLOUD instance
        console.log('ZEGOCLOUD meeting terminated');
      } catch (err) {
        console.error('Error destroying ZEGOCLOUD instance:', err);
      }
      this.zp = null;
    }

    // Stop local media stream
    if (this.localStream) {
      this.localStream.getTracks().forEach(track => {
        track.stop();
        console.log(`Stopped track: ${track.kind}`);
      });
      this.localStream = null;
    }
  }

  async checkMediaPermissions() {
    try {
      // Request camera and microphone permissions
      this.localStream = await navigator.mediaDevices.getUserMedia({
        video: true,
        audio: true,
      });
      this.errorMessage = null;
      this.startCall();
    } catch (err: any) {
      console.error('Media permission error:', err);
      if (err.name === 'NotAllowedError') {
        this.errorMessage =
          'Camera and microphone access denied. Please allow access in your browser settings.';
      } else if (err.name === 'NotFoundError') {
        this.errorMessage = 'No camera or microphone found on this device.';
      } else {
        this.errorMessage = 'Failed to access media devices. Please try again.';
      }
    }
  }

  startCall() {
    // WARNING: For prototyping only. Do NOT use ServerSecret in production.
    const appID = 1014901928;
    const serverSecret = '8e34368c5d47e24f2cf67a3f6f01a6a2';

    // Sanitize taskId to remove illegal characters for ZEGOCLOUD roomID
    const sanitizedTaskId = this.taskId
      .replace(/[^a-zA-Z0-9_-]/g, '-') // Replace invalid chars with hyphen
      .replace(/-+/g, '-') // Replace multiple hyphens with single
      .toLowerCase();

    try {
      const kitToken = ZegoUIKitPrebuilt.generateKitTokenForTest(
        appID,
        serverSecret,
        sanitizedTaskId,
        this.userId,
        this.userName || this.userId,
        24 * 60 * 60
      );

      this.zp = ZegoUIKitPrebuilt.create(kitToken);

      const container = document.getElementById('call-container') as HTMLElement | null;

      if (!container) {
        this.errorMessage = 'Call container not found';
        console.error('Call container not found');
        return;
      }

      this.zp.joinRoom({
        container,
        sharedLinks: [
          {
            name: 'Meeting Link',
            url: `${window.location.origin}/video-call/${encodeURIComponent(this.taskId)}`,
          },
        ],
        turnOnMicrophoneWhenJoining: true,
        turnOnCameraWhenJoining: true,
        showMyCameraToggleButton: true,
        showMyMicrophoneToggleButton: true,
        showAudioVideoSettingsButton: true,
        showScreenSharingButton: true,
        showTextChat: true,
        showUserList: true,
        maxUsers: 50,
        layout: 'Sidebar',
        showLayoutButton: true,
        scenario: {
          mode: ZegoUIKitPrebuilt.VideoConference,
          config: {
            role: ZegoUIKitPrebuilt.Host,
          },
        },
      });
      this.errorMessage = null;
    } catch (err) {
      this.errorMessage = 'Failed to initialize video call. Please try again.';
      console.error('Failed to initialize video call:', err);
    }
  }
}