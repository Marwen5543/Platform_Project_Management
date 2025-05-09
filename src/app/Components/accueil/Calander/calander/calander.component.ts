import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { PermissionService } from 'src/app/Service/PermissionService';
import { KeycloakService } from 'src/app/Service/KeycloakService';
import { TruncatePipe } from './TruncatePipe';

type Priority = 'high' | 'medium' | 'low';

interface Task {
  id?: number;
  title: string;
  description?: string;
  start: string | Date;
  end?: string | Date | null;
  allDay: boolean;
  category: string;
  priority: Priority;
  color: string;
  completed: boolean;
  creatorId: string;
}

interface WeekCell {
  date: Date;
  tasks: Task[];
  tasksByHour: Task[][];
}

interface Toast {
  id: number;
  message: string;
  type: 'success' | 'error';
  visible: boolean;
}

interface CalendarCell {
  date: Date;
  classes: string[];
  tasks: Task[];
  weekNumber: number;
}

@Component({
  selector: 'app-calander',
  templateUrl: './calander.component.html',
  styleUrls: ['./calander.component.css'],
  standalone: true,
  imports: [FormsModule, CommonModule, TruncatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CalanderComponent implements OnInit, OnDestroy {
  monthNames: string[] = [
    'Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
    'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'
  ];
  days = ['Dimanche', 'Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi'];
  currentDate: Date = new Date();
  currentView: 'month' | 'week' | 'day' = 'month';
  currentMonthName: string = this.monthNames[this.currentDate.getMonth()];
  currentYear: number = this.currentDate.getFullYear();
  tasks: Task[] = [];
  filteredTasks: Task[] = [];
  currentTask: Task | null = null;
  isModalOpen: boolean = false;
  isTaskDetailModalOpen: boolean = false;
  selectedTask: Task | null = null;
  isDeleteConfirmOpen: boolean = false;
  taskToDelete: number | null = null;
  taskData: Task = {
    title: '',
    description: '',
    start: '',
    end: null,
    allDay: false,
    category: 'work',
    priority: 'medium',
    color: '#dc2626',
    completed: false,
    creatorId: ''
  };
  toasts: Toast[] = [];
  todayTasks: number = 0;
  upcomingTasks: number = 0;
  completionRate: string = '0%';
  overdueTasks: number = 0;
  calendarCells: CalendarCell[] = [];
  weekCells: WeekCell[] = [];
  dayCells: { hour: number; tasks: Task[] }[] = [];
  searchQuery: string = '';
  canDeleteTasks: { [taskId: number]: boolean } = {};

  constructor(
    private permissionService: PermissionService,
    private keycloakService: KeycloakService,
    private cdr: ChangeDetectorRef
  ) {
    const tasksJson = localStorage.getItem('tasks');
    this.tasks = tasksJson
      ? JSON.parse(tasksJson)
          .map((task: any) => ({
            ...task,
            creatorId: task.creatorId || 'unknown',
            start: task.start ? new Date(task.start) : task.start,
            end: task.end ? new Date(task.end) : task.end
          }))
          .filter((task: Task) => task.id !== undefined && task.creatorId)
      : [];
    this.filteredTasks = [...this.tasks];
    this.currentMonthName = this.monthNames[this.currentDate.getMonth()];
    this.currentYear = this.currentDate.getFullYear();
  }

  ngOnInit(): void {
    this.updateMonthView();
    this.updateWeekView();
    this.updateDayView();
    this.updateStats();
    this.updateDeletePermissions();
  }

  ngOnDestroy(): void {}

  private updateDeletePermissions(): void {
    this.canDeleteTasks = {};
    this.tasks.forEach(task => {
      if (task.id !== undefined) {
        if (task.creatorId === 'unknown') {
          this.canDeleteTasks[task.id] = false;
        } else {
          this.permissionService.canDeleteTask(task.creatorId).subscribe({
            next: (canDelete) => {
              this.canDeleteTasks[task.id!] = canDelete;
              console.log(`Permission for task ${task.id}:`, canDelete, 'creatorId:', task.creatorId);
              this.cdr.markForCheck();
            },
            error: (err) => {
              console.error(`Failed to check delete permission for task ${task.id}:`, err);
              this.canDeleteTasks[task.id!] = false;
              this.cdr.markForCheck();
            }
          });
        }
      }
    });
  }

  private groupTasksByDate(tasks: Task[]): Record<string, Task[]> {
    const grouped: Record<string, Task[]> = {};
    tasks.forEach((task: Task) => {
      const dateKey = new Date(task.start).toDateString();
      if (!grouped[dateKey]) grouped[dateKey] = [];
      grouped[dateKey].push(task);
    });
    return grouped;
  }

  private sortTasks(tasks: Task[]): Task[] {
    return tasks.sort((a, b) => {
      const timeDiff = new Date(a.start).getTime() - new Date(b.start).getTime();
      if (timeDiff !== 0) return timeDiff;
      const priorityOrder: { [key in Priority]: number } = { high: 1, medium: 2, low: 3 };
      return priorityOrder[a.priority] - priorityOrder[b.priority];
    });
  }

  showToast(message: string, type: 'success' | 'error' = 'success'): void {
    const toast: Toast = {
      id: Date.now(),
      message,
      type,
      visible: false
    };
    this.toasts.push(toast);
    setTimeout(() => (toast.visible = true), 100);
    setTimeout(() => {
      toast.visible = false;
      setTimeout(() => (this.toasts = this.toasts.filter(t => t.id !== toast.id)), 300);
    }, 3000);
    this.cdr.markForCheck();
  }

  closeToast(id: number): void {
    const toast = this.toasts.find(t => t.id === id);
    if (toast) {
      toast.visible = false;
      setTimeout(() => (this.toasts = this.toasts.filter(t => t.id !== id)), 300);
    }
    this.cdr.markForCheck();
  }

  updateMonthView(tasksToRender: Task[] = this.filteredTasks): void {
    this.calendarCells = [];
    const year = this.currentDate.getFullYear();
    const month = this.currentDate.getMonth();
    const firstDay = new Date(year, month, 1).getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const groupedTasks = this.groupTasksByDate(tasksToRender);
    const today = new Date();

    const getWeekNumber = (date: Date): number => {
      const d = new Date(date);
      d.setHours(0, 0, 0, 0);
      d.setDate(d.getDate() + 4 - (d.getDay() || 7));
      const yearStart = new Date(d.getFullYear(), 0, 1);
      return Math.ceil((((d.getTime() - yearStart.getTime()) / 86400000) + 1) / 7);
    };

    for (let i = firstDay - 1; i >= 0; i--) {
      const dayNumber = new Date(year, month, 0).getDate() - i;
      const date = new Date(year, month - 1, dayNumber);
      const weekNumber = getWeekNumber(date);
      this.calendarCells.push({
        date,
        classes: ['outside-month'],
        tasks: this.sortTasks(groupedTasks[date.toDateString()] || []),
        weekNumber
      });
    }

    let currentRow = firstDay;
    for (let i = 1; i <= daysInMonth; i++) {
      const date = new Date(year, month, i);
      const weekNumber = getWeekNumber(date);
      const classes = [];
      if (
        i === today.getDate() &&
        month === today.getMonth() &&
        year === today.getFullYear()
      ) {
        classes.push('today');
      }
      if (date.getDay() === 0 || date.getDay() === 6) {
        classes.push('weekend');
      }
      this.calendarCells.push({
        date,
        classes,
        tasks: this.sortTasks(groupedTasks[date.toDateString()] || []),
        weekNumber
      });
      currentRow++;
    }

    const totalCellsFilled = firstDay + daysInMonth;
    const remainingCells = (7 - (totalCellsFilled % 7)) % 7;
    if (remainingCells > 0) {
      for (let i = 1; i <= remainingCells; i++) {
        const date = new Date(year, month + 1, i);
        const weekNumber = getWeekNumber(date);
        this.calendarCells.push({
          date,
          classes: ['outside-month'],
          tasks: this.sortTasks(groupedTasks[date.toDateString()] || []),
          weekNumber
        });
      }
    }

    this.currentMonthName = this.monthNames[this.currentDate.getMonth()];
    this.currentYear = this.currentDate.getFullYear();
    this.cdr.markForCheck();
  }

  updateWeekView(tasksToRender: Task[] = this.filteredTasks): void {
    this.weekCells = [];
    const startOfWeek = new Date(this.currentDate);
    startOfWeek.setDate(this.currentDate.getDate() - this.currentDate.getDay());
    startOfWeek.setHours(0, 0, 0, 0);

    const groupedTasks = this.groupTasksByDate(tasksToRender);

    for (let i = 0; i < 7; i++) {
      const date = new Date(startOfWeek);
      date.setDate(startOfWeek.getDate() + i);

      const tasksForDay = groupedTasks[date.toDateString()] || [];
      const tasksByHour: Task[][] = Array.from({ length: 24 }, () => []);
      tasksForDay.forEach(task => {
        const taskDate = new Date(task.start);
        const taskHour = taskDate.getHours();
        tasksByHour[taskHour].push(task);
      });

      this.weekCells.push({
        date,
        tasks: this.sortTasks(tasksForDay),
        tasksByHour
      });
    }
    this.cdr.markForCheck();
  }

  updateDayView(tasksToRender: Task[] = this.filteredTasks): void {
    this.dayCells = [];
    const currentDateStr = this.currentDate.toDateString();
    const groupedTasks = this.groupTasksByDate(tasksToRender);
    const dayTasks = this.sortTasks(groupedTasks[currentDateStr] || []);

    for (let i = 0; i < 24; i++) {
      this.dayCells.push({
        hour: i,
        tasks: dayTasks.filter(task => new Date(task.start).getHours() === i)
      });
    }
    this.cdr.markForCheck();
  }

  confirmDeleteTask(taskId: number): void {
    const task = this.tasks.find(t => t.id === taskId);
    if (!task) {
      this.showToast('Task not found', 'error');
      return;
    }

    this.permissionService.canDeleteTask(task.creatorId).subscribe({
      next: (canDelete) => {
        if (canDelete) {
          this.taskToDelete = taskId;
          this.isDeleteConfirmOpen = true;
        } else {
          this.showToast('You do not have permission to delete this task', 'error');
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error(`Failed to check delete permission for task ${taskId}:`, err);
        this.showToast('Error checking permissions', 'error');
        this.cdr.markForCheck();
      }
    });
  }

  deleteTask(): void {
    if (this.taskToDelete !== null) {
      this.tasks = this.tasks.filter(t => t.id !== this.taskToDelete);
      this.filteredTasks = this.filteredTasks.filter(t => t.id !== this.taskToDelete);
      localStorage.setItem('tasks', JSON.stringify(this.tasks));
      this.updateMonthView();
      this.updateWeekView();
      this.updateDayView();
      this.updateStats();
      this.updateDeletePermissions();
      this.showToast('Task deleted successfully');
    }
    this.closeDeleteConfirm();
  }

  closeDeleteConfirm(): void {
    this.isDeleteConfirmOpen = false;
    this.taskToDelete = null;
    this.cdr.markForCheck();
  }

  getDayAriaLabel(cell: CalendarCell): string {
    const dateStr = cell.date.toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' });
    const taskCount = cell.tasks.length;
    return `${dateStr}, ${taskCount} task${taskCount === 1 ? '' : 's'}`;
  }

  openTaskModalForDay(date: Date): void {
    this.keycloakService.getCurrentUser().subscribe({
      next: (currentUser) => {
        if (!currentUser) {
          this.showToast('You must be authenticated to create a task', 'error');
          this.cdr.markForCheck();
          return;
        }
        console.log(`openTaskModalForDay: Creating new task for user ${currentUser.userId}`);
        this.taskData = {
          title: '',
          description: '',
          start: date.toISOString().slice(0, 16),
          end: null,
          allDay: false,
          category: 'work',
          priority: 'medium',
          color: '#dc2626',
          completed: false,
          creatorId: currentUser.userId
        };
        this.currentTask = null;
        console.log('Opening modal for new task on day:', this.taskData);
        this.isModalOpen = true;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Error checking authentication for new task:', err);
        this.showToast('Error checking authentication', 'error');
        this.cdr.markForCheck();
      }
    });
  }

  openTaskModalForDayAndHour(date: Date, hour: number): void {
    this.keycloakService.getCurrentUser().subscribe({
      next: (currentUser) => {
        if (!currentUser) {
          this.showToast('You must be authenticated to create a task', 'error');
          this.cdr.markForCheck();
          return;
        }
        console.log(`openTaskModalForDayAndHour: Creating new task for user ${currentUser.userId}`);
        const taskStartDate = new Date(date);
        taskStartDate.setHours(hour, 0, 0, 0);
        const taskEndDate = new Date(taskStartDate);
        taskEndDate.setHours(hour + 1);
        this.taskData = {
          title: '',
          description: '',
          start: taskStartDate.toISOString().slice(0, 16),
          end: taskEndDate.toISOString().slice(0, 16),
          allDay: false,
          category: 'work',
          priority: 'medium',
          color: '#dc2626',
          completed: false,
          creatorId: currentUser.userId
        };
        this.currentTask = null;
        console.log('Opening modal for new task at hour:', this.taskData);
        this.isModalOpen = true;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Error checking authentication for new task:', err);
        this.showToast('Error checking authentication', 'error');
        this.cdr.markForCheck();
      }
    });
  }

  openTaskModal(task: Task | null = null): void {
    if (task) {
      console.log(`openTaskModal: Attempting to access task ${task.id}, creatorId: ${task.creatorId}`);
      this.keycloakService.getCurrentUser().subscribe({
        next: (currentUser) => {
          if (!currentUser) {
            this.showToast('You must be authenticated to view tasks', 'error');
            this.cdr.markForCheck();
            return;
          }
          if (task.creatorId === currentUser.userId) {
            console.log(`User ${currentUser.userId} is the creator, opening edit modal`);
            this.currentTask = { ...task };
            this.taskData = {
              ...task,
              creatorId: task.creatorId,
              start: task.start instanceof Date ? task.start.toISOString().slice(0, 16) : task.start,
              end: task.end instanceof Date ? task.end.toISOString().slice(0, 16) : task.end
            };
            this.isModalOpen = true;
          } else {
            console.log(`User ${currentUser.userId} is not the creator, opening task details modal`);
            this.selectedTask = {
              ...task,
              start: task.start instanceof Date ? task.start : new Date(task.start),
              end: task.end ? (task.end instanceof Date ? task.end : new Date(task.end)) : null
            };
            this.isTaskDetailModalOpen = true;
          }
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error(`Error checking user authentication for task ${task?.id}:`, err);
          this.showToast('Error checking authentication', 'error');
          this.cdr.markForCheck();
        }
      });
    } else {
      this.keycloakService.getCurrentUser().subscribe({
        next: (currentUser) => {
          if (!currentUser) {
            this.showToast('You must be authenticated to create a task', 'error');
            this.cdr.markForCheck();
            return;
          }
          console.log(`openTaskModal: Creating new task for user ${currentUser.userId}`);
          this.currentTask = null;
          this.taskData = {
            title: '',
            description: '',
            start: '',
            end: null,
            allDay: false,
            category: 'work',
            priority: 'medium',
            color: '#dc2626',
            completed: false,
            creatorId: currentUser.userId
          };
          this.isModalOpen = true;
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error('Error checking authentication for new task:', err);
          this.showToast('Error checking authentication', 'error');
          this.cdr.markForCheck();
        }
      });
    }
  }

  closeTaskDetailModal(): void {
    this.isTaskDetailModalOpen = false;
    this.selectedTask = null;
    this.cdr.markForCheck();
  }

  closeModal(): void {
    this.isModalOpen = false;
    this.currentTask = null;
    this.cdr.markForCheck();
  }

  saveTask(): void {
    if (!this.taskData.title) {
      this.showToast('Title is required', 'error');
      this.cdr.markForCheck();
      return;
    }
    if (!this.validateTaskDate()) {
      this.cdr.markForCheck();
      return;
    }
    this.keycloakService.getCurrentUser().subscribe({
      next: (currentUser) => {
        if (!currentUser) {
          this.showToast('User not authenticated', 'error');
          this.cdr.markForCheck();
          return;
        }
        console.log(`saveTask: Current user ${currentUser.userId}, task creatorId: ${this.currentTask?.creatorId || 'new'}`);
        if (this.currentTask && this.currentTask.id !== undefined) {
          if (!this.currentTask.creatorId || this.currentTask.creatorId === 'unknown') {
            console.error(`saveTask: Invalid creatorId for task ${this.currentTask.id}`);
            this.showToast('Invalid task creator', 'error');
            this.cdr.markForCheck();
            return;
          }
          this.permissionService.canEditTask(this.currentTask.creatorId).subscribe({
            next: (canEdit) => {
              console.log(`saveTask: Can edit task ${this.currentTask?.id ?? 'unknown'} (creator: ${this.currentTask?.creatorId ?? 'unknown'}): ${canEdit}`);
              if (!canEdit) {
                this.showToast('You do not have permission to edit this task', 'error');
                this.cdr.markForCheck();
                return;
              }
              this.saveTaskInternal(currentUser.userId);
            },
            error: (err) => {
              console.error(`Error checking edit permission for task ${this.currentTask?.id ?? 'unknown'}:`, err);
              this.showToast('Error checking permissions', 'error');
              this.cdr.markForCheck();
            }
          });
        } else {
          this.saveTaskInternal(currentUser.userId);
        }
      },
      error: (err) => {
        console.error('Error fetching current user:', err);
        this.showToast('Error checking authentication', 'error');
        this.cdr.markForCheck();
      }
    });
  }

  private saveTaskInternal(userId: string): void {
    const task: Task = {
      ...this.taskData,
      id: this.currentTask?.id ?? Date.now(),
      creatorId: this.currentTask?.creatorId ?? userId,
      start: this.taskData.start ? new Date(this.taskData.start) : this.taskData.start,
      end: this.taskData.end ? new Date(this.taskData.end) : null
    };
    console.log('Saving task:', task, 'Original creatorId:', this.currentTask?.creatorId);
    if (this.currentTask && this.currentTask.id !== undefined) {
      const index = this.tasks.findIndex(t => t.id === this.currentTask!.id);
      if (index !== -1) {
        this.tasks = [...this.tasks.slice(0, index), task, ...this.tasks.slice(index + 1)];
        this.filteredTasks = [...this.filteredTasks.slice(0, index), task, ...this.filteredTasks.slice(index + 1)];
      }
    } else {
      this.tasks = [...this.tasks, task];
      this.filteredTasks = [...this.tasks, task];
    }
    localStorage.setItem('tasks', JSON.stringify(this.tasks));
    this.closeModal();
    this.applyFilters();
    this.updateStats();
    this.updateDeletePermissions();
    this.showToast('Task saved successfully');
    this.cdr.markForCheck();
  }

  validateTaskDate(): boolean {
    if (!this.taskData.start) {
      this.showToast('Start time is required', 'error');
      return false;
    }
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const taskStartDate = new Date(this.taskData.start);
    if (taskStartDate < today) {
      this.showToast('Tasks cannot be scheduled in the past', 'error');
      return false;
    }
    return true;
  }

  applyFilters(): void {
    const categoryFilter = document.getElementById('category-filter') as HTMLSelectElement;
    const completedFilter = document.getElementById('completed-filter') as HTMLSelectElement;
    const priorityFilter = document.getElementById('priority-filter') as HTMLSelectElement;

    let filteredTasks = [...this.tasks];
    if (categoryFilter.value) {
      filteredTasks = filteredTasks.filter(t => t.category === categoryFilter.value);
    }
    if (completedFilter.value) {
      filteredTasks = filteredTasks.filter(t => t.completed === (completedFilter.value === 'true'));
    }
    if (priorityFilter.value) {
      filteredTasks = filteredTasks.filter(t => t.priority === priorityFilter.value);
    }
    if (this.searchQuery) {
      filteredTasks = filteredTasks.filter(t =>
        t.title.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        (t.description && t.description.toLowerCase().includes(this.searchQuery.toLowerCase()))
      );
    }

    this.filteredTasks = filteredTasks;
    this.updateMonthView();
    this.updateWeekView();
    this.updateDayView();
    this.cdr.markForCheck();
  }

  onSearchChange(query: string): void {
    this.searchQuery = query;
    this.applyFilters();
  }

  updateStats(): void {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const upcomingDate = new Date(today);
    upcomingDate.setDate(today.getDate() + 7);

    this.todayTasks = this.tasks.filter(t => new Date(t.start).toDateString() === today.toDateString()).length;
    this.upcomingTasks = this.tasks.filter(t => t.end && new Date(t.end) > today && new Date(t.end) <= upcomingDate).length;
    this.completionRate = this.tasks.length ? `${((this.tasks.filter(t => t.completed).length / this.tasks.length) * 100).toFixed(0)}%` : '0%';
    this.overdueTasks = this.tasks.filter(t => t.end && new Date(t.end) < today && !t.completed).length;
    this.cdr.markForCheck();
  }

  navigate(direction: 'prev' | 'next'): void {
    const month = this.currentDate.getMonth();
    this.currentDate.setMonth(direction === 'prev' ? month - 1 : month + 1);
    this.currentMonthName = this.monthNames[this.currentDate.getMonth()];
    this.currentYear = this.currentDate.getFullYear();
    this.switchView(this.currentView);
    this.cdr.markForCheck();
  }

  goToToday(): void {
    this.currentDate = new Date();
    this.currentMonthName = this.monthNames[this.currentDate.getMonth()];
    this.currentYear = this.currentDate.getFullYear();
    this.switchView(this.currentView);
    this.cdr.markForCheck();
  }

  toggleTheme(): void {
    document.body.classList.toggle('dark-mode');
    const toggleThemeBtn = document.getElementById('toggle-theme-btn') as HTMLButtonElement;
    toggleThemeBtn.textContent = document.body.classList.contains('dark-mode') ? '☀️' : '🌙';
    this.cdr.markForCheck();
  }

  switchView(view: 'month' | 'week' | 'day'): void {
    this.currentView = view;
    if (view === 'month') {
      this.updateMonthView();
    } else if (view === 'week') {
      this.updateWeekView();
    } else {
      this.updateDayView();
    }
    this.cdr.markForCheck();
  }

  handleKeydown(event: KeyboardEvent, taskId?: number): void {
    if (event.key === 'Enter' || event.key === ' ') {
      if (taskId !== undefined) {
        event.preventDefault();
        this.confirmDeleteTask(taskId);
      }
    } else if (event.key === 'Escape' && this.isDeleteConfirmOpen) {
      this.closeDeleteConfirm();
    }
  }
}