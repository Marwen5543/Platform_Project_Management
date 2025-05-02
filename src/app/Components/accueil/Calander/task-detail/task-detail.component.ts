import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common'; 

@Component({
  selector: 'app-task-detail',
  standalone: true,
  imports: [CommonModule], 
  template: `
    <div *ngIf="task" class="task-detail">
      <h2>{{ task.title }}</h2>
      <p><strong>Description:</strong> {{ task.description || 'No description' }}</p>
      <p><strong>Start:</strong> {{ task.start ? (task.start | date:'medium') : 'N/A' }}</p>
      <p><strong>End:</strong> {{ task.end ? (task.end | date:'medium') : 'N/A' }}</p>
      <p><strong>Category:</strong> {{ task.category }}</p>
      <p><strong>Priority:</strong> {{ task.priority }}</p>
      <p><strong>Status:</strong> {{ task.completed ? 'Completed' : 'Pending' }}</p>
      <button class="btn" (click)="closeTab()">Close</button>
    </div>
    <p *ngIf="!task">Task not found</p>
  `,
  styles: [`
    .task-detail { padding: 20px; max-width: 600px; margin: 0 auto; }
    .btn { padding: 8px 16px; cursor: pointer; }
  `]
})
export class TaskDetailComponent implements OnInit {
  task: any = null;

  constructor(private route: ActivatedRoute) {}

  ngOnInit(): void {
    const taskJson = localStorage.getItem('viewTaskDetail');
    if (taskJson) {
      this.task = JSON.parse(taskJson);
      // Ensure start and end are Date objects for DatePipe
      if (this.task.start) {
        this.task.start = new Date(this.task.start);
      }
      if (this.task.end) {
        this.task.end = new Date(this.task.end);
      }
    }
    // Optionally, use query param id to fetch task if needed
    this.route.queryParams.subscribe(params => {
      const id = params['id'];
      if (id && !this.task) {
        // Placeholder: Fetch task by id if not using localStorage
        console.log('Fetch task with id:', id);
      }
    });
  }

  closeTab(): void {
    window.close();
  }
}