export interface Task {
    id: number | null;
    title: string;
    description: string;
    startDate: string;
    endDate?: string;
    allDay: boolean;
    completed: boolean;
    color: string;
    category: string;
    priority?: string; 
    status?: string;   
    createdBy: string;
  }